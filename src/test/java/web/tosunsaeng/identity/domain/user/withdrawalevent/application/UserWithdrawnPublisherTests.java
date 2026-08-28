package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

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

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;

class UserWithdrawnPublisherTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant NOW = Instant.parse("2026-08-28T02:00:00Z");

	@Test
	void mapperEmitsOnlyVersionOneContractFields() {
		String payload = new String(
				new UserWithdrawnEventMapper(objectMapper()).serialize(event()),
				StandardCharsets.UTF_8
		);

		assertThat(payload)
				.contains("\"eventId\"", "\"schemaVersion\":1", "\"userId\"", "\"withdrawnAt\"")
				.doesNotContain("eventType", "firebase", "email", "phone", "provider", "credential");
	}

	@Test
	void publishesTwoHundredResponsesAndRetriesOnlyTransientFailures() {
		UserWithdrawnOutboxRepository successRepository = mock(UserWithdrawnOutboxRepository.class);
		UserWithdrawnDeliveryPort successPort = mock(UserWithdrawnDeliveryPort.class);
		UserWithdrawnOutbox success = claimedEvent();
		when(successRepository.claimNext(any(), eq(NOW), eq(NOW.plusSeconds(60))))
				.thenReturn(Optional.of(success));
		when(successPort.deliver(any())).thenReturn(204);
		when(successRepository.markPublished(
				eq(success.getEventId()), any(), eq(NOW), eq(NOW.plus(Duration.ofDays(30)))
		)).thenReturn(true);

		assertThat(publisher(successRepository, successPort).publishNext())
				.isEqualTo(UserWithdrawnPublisher.Outcome.PUBLISHED);

		UserWithdrawnOutboxRepository retryRepository = mock(UserWithdrawnOutboxRepository.class);
		UserWithdrawnDeliveryPort retryPort = mock(UserWithdrawnDeliveryPort.class);
		UserWithdrawnOutbox retry = claimedEvent();
		when(retryRepository.claimNext(any(), any(), any())).thenReturn(Optional.of(retry));
		when(retryPort.deliver(any())).thenReturn(429);
		when(retryRepository.scheduleRetry(
				eq(retry.getEventId()), any(), eq(UserWithdrawnFailureCode.HTTP_429),
				eq(NOW.plusSeconds(5))
		)).thenReturn(true);

		assertThat(publisher(retryRepository, retryPort).publishNext())
				.isEqualTo(UserWithdrawnPublisher.Outcome.RETRY_SCHEDULED);
	}

	@Test
	void payloadAndDeploymentConfigurationErrorsAreDeadLettered() {
		assertPermanentFailure(413, UserWithdrawnFailureCode.HTTP_413);
		assertPermanentFailure(401, UserWithdrawnFailureCode.HTTP_401);
		assertPermanentFailure(404, UserWithdrawnFailureCode.HTTP_404);
		assertPermanentFailure(418, UserWithdrawnFailureCode.HTTP_OTHER_4XX);
	}

	private void assertPermanentFailure(int status, UserWithdrawnFailureCode code) {
		UserWithdrawnOutboxRepository repository = mock(UserWithdrawnOutboxRepository.class);
		UserWithdrawnDeliveryPort port = mock(UserWithdrawnDeliveryPort.class);
		UserWithdrawnOutbox event = claimedEvent();
		when(repository.claimNext(any(), any(), any())).thenReturn(Optional.of(event));
		when(port.deliver(any())).thenReturn(status);
		when(repository.markDeadLetter(
				eq(event.getEventId()), any(), eq(code), eq(NOW),
				eq(NOW.plus(Duration.ofDays(90)))
		)).thenReturn(true);

		assertThat(publisher(repository, port).publishNext())
				.isEqualTo(UserWithdrawnPublisher.Outcome.DEAD_LETTERED);
		verify(repository).markDeadLetter(
				eq(event.getEventId()), any(), eq(code), eq(NOW),
				eq(NOW.plus(Duration.ofDays(90)))
		);
	}

	private UserWithdrawnPublisher publisher(
			UserWithdrawnOutboxRepository repository,
			UserWithdrawnDeliveryPort port
	) {
		return new UserWithdrawnPublisher(
				Duration.ofSeconds(60),
				3,
				Duration.ofDays(30),
				Duration.ofDays(90),
				repository,
				new UserWithdrawnEventMapper(objectMapper()),
				port,
				new UserWithdrawnRetryPolicy(() -> 0.5),
				new SimpleMeterRegistry(),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	private ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}

	private UserWithdrawnOutbox claimedEvent() {
		UserWithdrawnOutbox event = event();
		ReflectionTestUtils.setField(event, "attemptCount", 1);
		return event;
	}

	private UserWithdrawnOutbox event() {
		return UserWithdrawnOutbox.create(USER_ID, NOW.minusSeconds(30));
	}
}
