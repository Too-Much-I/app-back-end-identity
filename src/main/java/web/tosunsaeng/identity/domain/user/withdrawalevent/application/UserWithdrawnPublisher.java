package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;

public final class UserWithdrawnPublisher {

	public enum Outcome { NONE, PUBLISHED, RETRY_SCHEDULED, DEAD_LETTERED, LEASE_LOST }

	private final String leaseOwner = UUID.randomUUID().toString();
	private final Duration leaseDuration;
	private final int maxAttempts;
	private final Duration publishedRetention;
	private final Duration deadLetterReview;
	private final UserWithdrawnOutboxRepository repository;
	private final UserWithdrawnEventMapper mapper;
	private final UserWithdrawnDeliveryPort deliveryPort;
	private final UserWithdrawnRetryPolicy retryPolicy;
	private final MeterRegistry meterRegistry;
	private final Clock clock;

	public UserWithdrawnPublisher(
			Duration leaseDuration,
			int maxAttempts,
			Duration publishedRetention,
			Duration deadLetterReview,
			UserWithdrawnOutboxRepository repository,
			UserWithdrawnEventMapper mapper,
			UserWithdrawnDeliveryPort deliveryPort,
			UserWithdrawnRetryPolicy retryPolicy,
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
		this.repository = Objects.requireNonNull(repository);
		this.mapper = Objects.requireNonNull(mapper);
		this.deliveryPort = Objects.requireNonNull(deliveryPort);
		this.retryPolicy = Objects.requireNonNull(retryPolicy);
		this.meterRegistry = Objects.requireNonNull(meterRegistry);
		this.clock = Objects.requireNonNull(clock);
	}

	public Outcome publishNext() {
		Instant now = clock.instant();
		Optional<UserWithdrawnOutbox> claimed = repository.claimNext(
				leaseOwner,
				now,
				now.plus(leaseDuration)
		);
		if (claimed.isEmpty()) {
			return Outcome.NONE;
		}
		UserWithdrawnOutbox event = claimed.orElseThrow();
		try {
			return handleStatus(event, deliveryPort.deliver(mapper.serialize(event)), now);
		} catch (IllegalArgumentException exception) {
			return deadLetter(event, UserWithdrawnFailureCode.INVALID_PAYLOAD, now);
		} catch (UserWithdrawnDeliveryException exception) {
			UserWithdrawnFailureCode code = switch (exception.kind()) {
				case CREDENTIAL_UNAVAILABLE -> UserWithdrawnFailureCode.CREDENTIAL_UNAVAILABLE;
				case TIMEOUT -> UserWithdrawnFailureCode.TIMEOUT;
				case CONNECTION -> UserWithdrawnFailureCode.CONNECTION_ERROR;
			};
			return retryOrDeadLetter(event, code, now);
		} catch (RuntimeException exception) {
			return retryOrDeadLetter(event, UserWithdrawnFailureCode.DELIVERY_ERROR, now);
		}
	}

	private Outcome handleStatus(UserWithdrawnOutbox event, int status, Instant now) {
		if (status >= 200 && status < 300) {
			boolean updated = repository.markPublished(
					event.getEventId(),
					leaseOwner,
					now,
					now.plus(publishedRetention)
			);
			if (updated && !event.getWithdrawnAt().isAfter(now)) {
				meterRegistry.timer("identity.user_withdrawn.delivery_lag")
						.record(Duration.between(event.getWithdrawnAt(), now));
			}
			return record(updated ? Outcome.PUBLISHED : Outcome.LEASE_LOST, null);
		}
		UserWithdrawnFailureCode code = statusCode(status);
		if (status == 408 || status == 425 || status == 429 || status >= 500) {
			return retryOrDeadLetter(event, code, now);
		}
		return deadLetter(event, code, now);
	}

	private Outcome retryOrDeadLetter(
			UserWithdrawnOutbox event,
			UserWithdrawnFailureCode code,
			Instant now
	) {
		if (event.getAttemptCount() >= maxAttempts) {
			return deadLetter(event, code, now);
		}
		boolean updated = repository.scheduleRetry(
				event.getEventId(),
				leaseOwner,
				code,
				now.plus(retryPolicy.delay(event.getAttemptCount()))
		);
		return record(updated ? Outcome.RETRY_SCHEDULED : Outcome.LEASE_LOST, code);
	}

	private Outcome deadLetter(
			UserWithdrawnOutbox event,
			UserWithdrawnFailureCode code,
			Instant now
	) {
		boolean updated = repository.markDeadLetter(
				event.getEventId(),
				leaseOwner,
				code,
				now,
				now.plus(deadLetterReview)
		);
		return record(updated ? Outcome.DEAD_LETTERED : Outcome.LEASE_LOST, code);
	}

	private Outcome record(Outcome outcome, UserWithdrawnFailureCode failureCode) {
		meterRegistry.counter(
				"identity.user_withdrawn.publisher",
				"schemaVersion", Integer.toString(UserWithdrawnOutbox.SCHEMA_VERSION),
				"outcome", outcome.name(),
				"failureCode", failureCode == null ? "NONE" : failureCode.name()
		).increment();
		return outcome;
	}

	private static UserWithdrawnFailureCode statusCode(int status) {
		return switch (status) {
			case 400 -> UserWithdrawnFailureCode.HTTP_400;
			case 401 -> UserWithdrawnFailureCode.HTTP_401;
			case 403 -> UserWithdrawnFailureCode.HTTP_403;
			case 404 -> UserWithdrawnFailureCode.HTTP_404;
			case 405 -> UserWithdrawnFailureCode.HTTP_405;
			case 408 -> UserWithdrawnFailureCode.HTTP_408;
			case 409 -> UserWithdrawnFailureCode.HTTP_409;
			case 413 -> UserWithdrawnFailureCode.HTTP_413;
			case 422 -> UserWithdrawnFailureCode.HTTP_422;
			case 425 -> UserWithdrawnFailureCode.HTTP_425;
			case 429 -> UserWithdrawnFailureCode.HTTP_429;
			default -> status >= 300 && status < 400
					? UserWithdrawnFailureCode.HTTP_3XX
					: status >= 400 && status < 500
							? UserWithdrawnFailureCode.HTTP_OTHER_4XX
							: status >= 500
									? UserWithdrawnFailureCode.HTTP_5XX
									: UserWithdrawnFailureCode.HTTP_OTHER;
		};
	}

	private static Duration requirePositive(Duration value, String name) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(name + " must be positive");
		}
		return value;
	}
}
