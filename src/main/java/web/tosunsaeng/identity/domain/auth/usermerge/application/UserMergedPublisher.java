package web.tosunsaeng.identity.domain.auth.usermerge.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;

import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedFailureCode;
import web.tosunsaeng.identity.domain.auth.usermerge.repository.UserMergedOutboxRepository;

public final class UserMergedPublisher {

	public enum Outcome { NONE, PUBLISHED, RETRY_SCHEDULED, DEAD_LETTERED, LEASE_LOST }

	private final String leaseOwner = UUID.randomUUID().toString();
	private final Duration leaseDuration;
	private final int maxAttempts;
	private final Duration publishedRetention;
	private final Duration deadLetterReview;
	private final UserMergedOutboxRepository outboxRepository;
	private final UserMergedEventMapper mapper;
	private final UserMergedDeliveryPort deliveryPort;
	private final UserMergedRetryPolicy retryPolicy;
	private final MeterRegistry meterRegistry;
	private final Clock clock;

	public UserMergedPublisher(
			Duration leaseDuration,
			int maxAttempts,
			Duration publishedRetention,
			Duration deadLetterReview,
			UserMergedOutboxRepository outboxRepository,
			UserMergedEventMapper mapper,
			UserMergedDeliveryPort deliveryPort,
			UserMergedRetryPolicy retryPolicy,
			MeterRegistry meterRegistry,
			Clock clock
	) {
		this.leaseDuration = requirePositive(leaseDuration, "leaseDuration");
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be positive");
		}
		this.maxAttempts = maxAttempts;
		this.publishedRetention = requirePositive(publishedRetention, "publishedRetention");
		this.deadLetterReview = requirePositive(deadLetterReview, "deadLetterReview");
		this.outboxRepository = Objects.requireNonNull(outboxRepository);
		this.mapper = Objects.requireNonNull(mapper);
		this.deliveryPort = Objects.requireNonNull(deliveryPort);
		this.retryPolicy = Objects.requireNonNull(retryPolicy);
		this.meterRegistry = Objects.requireNonNull(meterRegistry);
		this.clock = Objects.requireNonNull(clock);
	}

	public Outcome publishNext() {
		Instant now = clock.instant();
		Optional<UserMergedOutbox> claimed = outboxRepository.claimNext(
				leaseOwner,
				now,
				now.plus(leaseDuration)
		);
		if (claimed.isEmpty()) {
			return Outcome.NONE;
		}
		UserMergedOutbox event = claimed.orElseThrow();
		try {
			return handleStatus(event, deliveryPort.deliver(mapper.serialize(event)), now);
		} catch (IllegalArgumentException exception) {
			return deadLetter(event, UserMergedFailureCode.INVALID_PAYLOAD, now);
		} catch (UserMergedDeliveryException exception) {
			UserMergedFailureCode code = switch (exception.kind()) {
				case CREDENTIAL_UNAVAILABLE -> UserMergedFailureCode.CREDENTIAL_UNAVAILABLE;
				case TIMEOUT -> UserMergedFailureCode.TIMEOUT;
				case CONNECTION -> UserMergedFailureCode.CONNECTION_ERROR;
			};
			return retryOrDeadLetter(event, code, now);
		} catch (RuntimeException exception) {
			return retryOrDeadLetter(event, UserMergedFailureCode.DELIVERY_ERROR, now);
		}
	}

	private Outcome handleStatus(UserMergedOutbox event, int status, Instant now) {
		if (status >= 200 && status < 300) {
			boolean updated = outboxRepository.markPublished(
					event.getEventId(),
					leaseOwner,
					now,
					now.plus(publishedRetention)
			);
			return record(updated ? Outcome.PUBLISHED : Outcome.LEASE_LOST, null);
		}
		UserMergedFailureCode code = statusCode(status);
		if (status == 408 || status == 425 || status == 429 || status >= 500) {
			return retryOrDeadLetter(event, code, now);
		}
		return deadLetter(event, code, now);
	}

	private Outcome retryOrDeadLetter(
			UserMergedOutbox event,
			UserMergedFailureCode code,
			Instant now
	) {
		if (event.getAttemptCount() >= maxAttempts) {
			return deadLetter(event, code, now);
		}
		boolean updated = outboxRepository.scheduleRetry(
				event.getEventId(),
				leaseOwner,
				code,
				now.plus(retryPolicy.delay(event.getAttemptCount()))
		);
		return record(updated ? Outcome.RETRY_SCHEDULED : Outcome.LEASE_LOST, code);
	}

	private Outcome deadLetter(
			UserMergedOutbox event,
			UserMergedFailureCode code,
			Instant now
	) {
		boolean updated = outboxRepository.markDeadLetter(
				event.getEventId(),
				leaseOwner,
				code,
				now,
				now.plus(deadLetterReview)
		);
		return record(updated ? Outcome.DEAD_LETTERED : Outcome.LEASE_LOST, code);
	}

	private Outcome record(Outcome outcome, UserMergedFailureCode failureCode) {
		meterRegistry.counter(
				"identity.user_merged.publisher",
				"schemaVersion", Integer.toString(UserMergedOutbox.SCHEMA_VERSION),
				"outcome", outcome.name(),
				"failureCode", failureCode == null ? "NONE" : failureCode.name()
		).increment();
		return outcome;
	}

	private static UserMergedFailureCode statusCode(int status) {
		return switch (status) {
			case 400 -> UserMergedFailureCode.HTTP_400;
			case 401 -> UserMergedFailureCode.HTTP_401;
			case 403 -> UserMergedFailureCode.HTTP_403;
			case 408 -> UserMergedFailureCode.HTTP_408;
			case 409 -> UserMergedFailureCode.HTTP_409;
			case 422 -> UserMergedFailureCode.HTTP_422;
			case 425 -> UserMergedFailureCode.HTTP_425;
			case 429 -> UserMergedFailureCode.HTTP_429;
			default -> status >= 300 && status < 400
					? UserMergedFailureCode.HTTP_3XX
					: status >= 500
							? UserMergedFailureCode.HTTP_5XX
							: UserMergedFailureCode.HTTP_OTHER;
		};
	}

	private static Duration requirePositive(Duration value, String name) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(name + " must be positive");
		}
		return value;
	}
}
