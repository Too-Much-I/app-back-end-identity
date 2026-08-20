package web.tosunsaeng.identity.domain.auth.usermerge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedFailureCode;
import web.tosunsaeng.identity.domain.auth.usermerge.repository.UserMergedOutboxRepository;

class UserMergedPublisherTests {

	private static final String SOURCE_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String TARGET_ID = "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e";
	private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

	@Test
	void mapperEmitsOnlyVersionOneContractFields() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		String payload = new String(
				new UserMergedEventMapper(objectMapper).serialize(event()),
				StandardCharsets.UTF_8
		);

		assertThat(payload)
				.contains("\"eventId\"", "\"schemaVersion\":1", "\"sourceUserId\"", "\"targetUserId\"", "\"occurredAt\"")
				.doesNotContain("firebase", "email", "phone", "providerSubject", "credential");
	}

	@Test
	void successfulDeliveryMarksPublishedAfterAtomicClaim() {
		UserMergedOutboxRepository repository = mock(UserMergedOutboxRepository.class);
		UserMergedDeliveryPort port = mock(UserMergedDeliveryPort.class);
		UserMergedOutbox event = claimedEvent();
		when(repository.claimNext(any(), eq(NOW), eq(NOW.plusSeconds(60))))
				.thenReturn(Optional.of(event));
		when(port.deliver(any())).thenReturn(204);
		when(repository.markPublished(
				eq(event.getEventId()), any(), eq(NOW), eq(NOW.plus(Duration.ofDays(30)))
		)).thenReturn(true);

		assertThat(publisher(repository, port).publishNext())
				.isEqualTo(UserMergedPublisher.Outcome.PUBLISHED);
		verify(repository).claimNext(any(), eq(NOW), eq(NOW.plusSeconds(60)));
	}

	@Test
	void retryableAndPermanentFailuresAreSeparated() {
		UserMergedOutboxRepository retryRepository = mock(UserMergedOutboxRepository.class);
		UserMergedDeliveryPort retryPort = mock(UserMergedDeliveryPort.class);
		UserMergedOutbox retryEvent = claimedEvent();
		when(retryRepository.claimNext(any(), any(), any()))
				.thenReturn(Optional.of(retryEvent));
		when(retryPort.deliver(any())).thenReturn(503);
		when(retryRepository.scheduleRetry(
				eq(retryEvent.getEventId()),
				any(),
				eq(UserMergedFailureCode.HTTP_5XX),
				eq(NOW.plusSeconds(5))
		)).thenReturn(true);
		assertThat(publisher(retryRepository, retryPort).publishNext())
				.isEqualTo(UserMergedPublisher.Outcome.RETRY_SCHEDULED);

		UserMergedOutboxRepository deadRepository = mock(UserMergedOutboxRepository.class);
		UserMergedDeliveryPort deadPort = mock(UserMergedDeliveryPort.class);
		UserMergedOutbox deadEvent = claimedEvent();
		when(deadRepository.claimNext(any(), any(), any()))
				.thenReturn(Optional.of(deadEvent));
		when(deadPort.deliver(any())).thenReturn(400);
		when(deadRepository.markDeadLetter(
				eq(deadEvent.getEventId()),
				any(),
				eq(UserMergedFailureCode.HTTP_400),
				eq(NOW),
				eq(NOW.plus(Duration.ofDays(90)))
		)).thenReturn(true);
		assertThat(publisher(deadRepository, deadPort).publishNext())
				.isEqualTo(UserMergedPublisher.Outcome.DEAD_LETTERED);
	}

	private UserMergedPublisher publisher(
			UserMergedOutboxRepository repository,
			UserMergedDeliveryPort port
	) {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		return new UserMergedPublisher(
				Duration.ofSeconds(60),
				3,
				Duration.ofDays(30),
				Duration.ofDays(90),
				repository,
				new UserMergedEventMapper(objectMapper),
				port,
				new UserMergedRetryPolicy(() -> 0.5),
				new SimpleMeterRegistry(),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	private UserMergedOutbox claimedEvent() {
		UserMergedOutbox event = event();
		ReflectionTestUtils.setField(event, "attemptCount", 1);
		return event;
	}

	private UserMergedOutbox event() {
		return UserMergedOutbox.create(SOURCE_ID, TARGET_ID, NOW);
	}
}
