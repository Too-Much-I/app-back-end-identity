package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingFailureCode;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingDeliveryScopeStateRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;

class PhoneEligibilityBindingPublisherTests {

	private static final Instant NOW = Instant.parse("2026-08-14T06:00:00Z");
	private static final String USER_ID = "00000000-0000-4000-8000-000000000096";
	private static final String SCOPE = "opaque-scope-v1";

	@Test
	void serializesVerifiedAndRevokedSchemaV1WithoutLeakingCandidatesFromToString() {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		PhoneEligibilityBindingEventMapper mapper = new PhoneEligibilityBindingEventMapper(objectMapper);
		PhoneEligibilityBindingOutbox verified = verified();

		String json = new String(mapper.serialize(verified), StandardCharsets.UTF_8);

		assertThat(json).contains(
				"\"eventType\":\"PhoneEligibilityBindingVerified\"",
				"\"schemaVersion\":1",
				"\"producer\":\"identity\"",
				"\"bindingRevision\":1",
				"\"fingerprintCandidates\":[{\"keyVersion\":\"v1\",\"value\":\""
		);
		assertThat(json).doesNotContain("revokedAt");
		assertThat(verified.toString()).doesNotContain("C".repeat(43));

		PhoneEligibilityBindingOutbox revoked = PhoneEligibilityBindingOutbox.createRevoked(
				USER_ID, SCOPE, 2, NOW, NOW);
		String revokedJson = new String(mapper.serialize(revoked), StandardCharsets.UTF_8);
		assertThat(revokedJson)
				.contains("\"eventType\":\"PhoneEligibilityBindingRevoked\"", "\"revokedAt\"")
				.doesNotContain("verifiedAt", "fingerprintCandidates");
	}

	@Test
	void validatesEventShapeAndRetryCapWithDeterministicJitter() {
		assertThatThrownBy(() -> PhoneEligibilityBindingOutbox.createVerified(
				USER_ID, SCOPE, 0, List.of(candidate()), NOW, NOW))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> PhoneEligibilityBindingOutbox.createRevoked(
				USER_ID, SCOPE, 2, NOW, NOW.minusSeconds(1)))
				.isInstanceOf(IllegalArgumentException.class);

		PhoneEligibilityRetryPolicy policy = new PhoneEligibilityRetryPolicy(() -> 0.5);
		assertThat(policy.delay(1)).isEqualTo(Duration.ofSeconds(5));
		assertThat(policy.delay(12)).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void publishesOnlyAfterTwoXxAndUsesThirtyDayCleanup() {
		Fixture fixture = fixture(payload -> 204);
		PhoneEligibilityBindingOutbox event = claimed(verified(), 1);
		when(fixture.outboxRepository.claimNext(eq(SCOPE), any(), eq(NOW), eq(NOW.plusSeconds(60))))
				.thenReturn(Optional.of(event));
		when(fixture.outboxRepository.markPublished(eq(event.getEventId()), any(), eq(NOW),
				eq(NOW.plus(Duration.ofDays(30))))).thenReturn(true);

		assertThat(fixture.publisher.publishNext())
				.isEqualTo(PhoneEligibilityBindingPublisher.Outcome.PUBLISHED);
		verify(fixture.outboxRepository, never()).scheduleRetry(any(), any(), any(), any());
	}

	@Test
	void retriesTimeoutAndDeadLettersAtMaximumAttempt() {
		PhoneEligibilityBindingDeliveryPort timeout = payload -> {
			throw new PhoneEligibilityBindingDeliveryException(
					PhoneEligibilityBindingDeliveryException.Kind.TIMEOUT, null);
		};
		Fixture fixture = fixture(timeout);
		PhoneEligibilityBindingOutbox first = claimed(verified(), 1);
		when(fixture.outboxRepository.claimNext(any(), any(), any(), any()))
				.thenReturn(Optional.of(first));
		when(fixture.outboxRepository.scheduleRetry(eq(first.getEventId()), any(),
				eq(PhoneEligibilityBindingFailureCode.TIMEOUT), eq(NOW.plusSeconds(5))))
				.thenReturn(true);
		assertThat(fixture.publisher.publishNext())
				.isEqualTo(PhoneEligibilityBindingPublisher.Outcome.RETRY_SCHEDULED);

		PhoneEligibilityBindingOutbox finalAttempt = claimed(verified(), 12);
		when(fixture.outboxRepository.claimNext(any(), any(), any(), any()))
				.thenReturn(Optional.of(finalAttempt));
		when(fixture.outboxRepository.markDeadLetter(eq(finalAttempt.getEventId()), any(),
				eq(PhoneEligibilityBindingFailureCode.TIMEOUT), eq(NOW),
				eq(NOW.plus(Duration.ofDays(90))))).thenReturn(true);
		assertThat(fixture.publisher.publishNext())
				.isEqualTo(PhoneEligibilityBindingPublisher.Outcome.DEAD_LETTERED);
	}

	@Test
	void authenticationFailureDeadLettersAndPausesScope() {
		Fixture fixture = fixture(payload -> 403);
		PhoneEligibilityBindingOutbox event = claimed(verified(), 1);
		when(fixture.outboxRepository.claimNext(any(), any(), any(), any()))
				.thenReturn(Optional.of(event));
		when(fixture.outboxRepository.markDeadLetter(eq(event.getEventId()), any(),
				eq(PhoneEligibilityBindingFailureCode.HTTP_403), eq(NOW), any()))
				.thenReturn(true);

		assertThat(fixture.publisher.publishNext())
				.isEqualTo(PhoneEligibilityBindingPublisher.Outcome.SCOPE_PAUSED);
		verify(fixture.scopeStateRepository).save(any());
	}

	private Fixture fixture(PhoneEligibilityBindingDeliveryPort deliveryPort) {
		PhoneEligibilityBindingOutboxRepository outboxRepository = mock(
				PhoneEligibilityBindingOutboxRepository.class);
		PhoneEligibilityBindingDeliveryScopeStateRepository scopeStateRepository = mock(
				PhoneEligibilityBindingDeliveryScopeStateRepository.class);
		PhoneEligibilityBindingPublisher publisher = new PhoneEligibilityBindingPublisher(
				SCOPE, Duration.ofSeconds(60), 12, Duration.ofDays(30), Duration.ofDays(90),
				outboxRepository, scopeStateRepository,
				new PhoneEligibilityBindingEventMapper(new ObjectMapper().findAndRegisterModules()),
				deliveryPort, new PhoneEligibilityRetryPolicy(() -> 0.5),
				new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC));
		return new Fixture(publisher, outboxRepository, scopeStateRepository);
	}

	private PhoneEligibilityBindingOutbox verified() {
		return PhoneEligibilityBindingOutbox.createVerified(
				USER_ID, SCOPE, 1, List.of(candidate()), NOW, NOW);
	}

	private PhoneEligibilityFingerprintCandidate candidate() {
		return new PhoneEligibilityFingerprintCandidate("v1", "C".repeat(43));
	}

	private PhoneEligibilityBindingOutbox claimed(PhoneEligibilityBindingOutbox event, int attempts) {
		ReflectionTestUtils.setField(event, "attemptCount", attempts);
		return event;
	}

	private record Fixture(
			PhoneEligibilityBindingPublisher publisher,
			PhoneEligibilityBindingOutboxRepository outboxRepository,
			PhoneEligibilityBindingDeliveryScopeStateRepository scopeStateRepository
	) {
	}
}
