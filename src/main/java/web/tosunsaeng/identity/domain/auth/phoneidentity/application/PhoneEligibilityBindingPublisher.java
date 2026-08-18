package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingDeliveryScopeState;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingFailureCode;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingDeliveryScopeStateRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;

public final class PhoneEligibilityBindingPublisher {

	public enum Outcome { NONE, PUBLISHED, RETRY_SCHEDULED, DEAD_LETTERED, SCOPE_PAUSED, LEASE_LOST }

	private final String consumerScopeId;
	private final String leaseOwner = UUID.randomUUID().toString();
	private final Duration leaseDuration;
	private final int maxAttempts;
	private final Duration publishedRetention;
	private final Duration deadLetterReview;
	private final PhoneEligibilityBindingOutboxRepository outboxRepository;
	private final PhoneEligibilityBindingDeliveryScopeStateRepository scopeStateRepository;
	private final PhoneEligibilityBindingEventMapper mapper;
	private final PhoneEligibilityBindingDeliveryPort deliveryPort;
	private final PhoneEligibilityRetryPolicy retryPolicy;
	private final MeterRegistry meterRegistry;
	private final Clock clock;

	public PhoneEligibilityBindingPublisher(
			String consumerScopeId, Duration leaseDuration, int maxAttempts,
			Duration publishedRetention, Duration deadLetterReview,
			PhoneEligibilityBindingOutboxRepository outboxRepository,
			PhoneEligibilityBindingDeliveryScopeStateRepository scopeStateRepository,
			PhoneEligibilityBindingEventMapper mapper,
			PhoneEligibilityBindingDeliveryPort deliveryPort,
			PhoneEligibilityRetryPolicy retryPolicy, MeterRegistry meterRegistry, Clock clock
	) {
		this.consumerScopeId = requireText(consumerScopeId, "consumerScopeId");
		this.leaseDuration = requirePositive(leaseDuration, "leaseDuration");
		if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
		this.maxAttempts = maxAttempts;
		this.publishedRetention = requirePositive(publishedRetention, "publishedRetention");
		this.deadLetterReview = requirePositive(deadLetterReview, "deadLetterReview");
		this.outboxRepository = Objects.requireNonNull(outboxRepository);
		this.scopeStateRepository = Objects.requireNonNull(scopeStateRepository);
		this.mapper = Objects.requireNonNull(mapper);
		this.deliveryPort = Objects.requireNonNull(deliveryPort);
		this.retryPolicy = Objects.requireNonNull(retryPolicy);
		this.meterRegistry = Objects.requireNonNull(meterRegistry);
		this.clock = Objects.requireNonNull(clock);
	}

	public Outcome publishNext() {
		if (scopeStateRepository.existsById(consumerScopeId)) return Outcome.SCOPE_PAUSED;
		Instant now = clock.instant();
		Optional<PhoneEligibilityBindingOutbox> claimed = outboxRepository.claimNext(
				consumerScopeId, leaseOwner, now, now.plus(leaseDuration));
		if (claimed.isEmpty()) return Outcome.NONE;
		PhoneEligibilityBindingOutbox event = claimed.get();
		try {
			byte[] payload = mapper.serialize(event);
			int status = deliveryPort.deliver(payload);
			return handleStatus(event, status, now);
		} catch (IllegalArgumentException exception) {
			return deadLetter(event, PhoneEligibilityBindingFailureCode.INVALID_PAYLOAD, now, false);
		} catch (PhoneEligibilityBindingDeliveryException exception) {
			PhoneEligibilityBindingFailureCode code = switch (exception.kind()) {
				case CREDENTIAL_UNAVAILABLE -> PhoneEligibilityBindingFailureCode.CREDENTIAL_UNAVAILABLE;
				case TIMEOUT -> PhoneEligibilityBindingFailureCode.TIMEOUT;
				case CONNECTION -> PhoneEligibilityBindingFailureCode.CONNECTION_ERROR;
			};
			return retryOrDeadLetter(event, code, now);
		} catch (RuntimeException exception) {
			return retryOrDeadLetter(event, PhoneEligibilityBindingFailureCode.DELIVERY_ERROR, now);
		}
	}

	private Outcome handleStatus(PhoneEligibilityBindingOutbox event, int status, Instant now) {
		if (status >= 200 && status < 300) {
			boolean updated = outboxRepository.markPublished(
					event.getEventId(), leaseOwner, now, now.plus(publishedRetention));
			return record(event, updated ? Outcome.PUBLISHED : Outcome.LEASE_LOST, null);
		}
		PhoneEligibilityBindingFailureCode code = statusCode(status);
		if (status == 401 || status == 403) return deadLetter(event, code, now, true);
		if (status == 408 || status == 425 || status == 429 || status >= 500) {
			return retryOrDeadLetter(event, code, now);
		}
		return deadLetter(event, code, now, false);
	}

	private Outcome retryOrDeadLetter(PhoneEligibilityBindingOutbox event,
			PhoneEligibilityBindingFailureCode code, Instant now) {
		if (event.getAttemptCount() >= maxAttempts) return deadLetter(event, code, now, false);
		boolean updated = outboxRepository.scheduleRetry(event.getEventId(), leaseOwner, code,
				now.plus(retryPolicy.delay(event.getAttemptCount())));
		return record(event, updated ? Outcome.RETRY_SCHEDULED : Outcome.LEASE_LOST, code);
	}

	private Outcome deadLetter(PhoneEligibilityBindingOutbox event,
			PhoneEligibilityBindingFailureCode code, Instant now, boolean pauseScope) {
		boolean updated = outboxRepository.markDeadLetter(event.getEventId(), leaseOwner, code,
				now, now.plus(deadLetterReview));
		if (!updated) return record(event, Outcome.LEASE_LOST, code);
		if (pauseScope) {
			scopeStateRepository.save(PhoneEligibilityBindingDeliveryScopeState.paused(
					consumerScopeId, code, now));
			return record(event, Outcome.SCOPE_PAUSED, code);
		}
		return record(event, Outcome.DEAD_LETTERED, code);
	}

	private Outcome record(PhoneEligibilityBindingOutbox event, Outcome outcome,
			PhoneEligibilityBindingFailureCode failureCode) {
		meterRegistry.counter("identity.phone_eligibility.publisher",
				"eventType", event.getEventType().name(),
				"schemaVersion", Integer.toString(event.getSchemaVersion()),
				"outcome", outcome.name(),
				"failureCode", failureCode == null ? "NONE" : failureCode.name()).increment();
		return outcome;
	}

	private static PhoneEligibilityBindingFailureCode statusCode(int status) {
		return switch (status) {
			case 400 -> PhoneEligibilityBindingFailureCode.HTTP_400;
			case 401 -> PhoneEligibilityBindingFailureCode.HTTP_401;
			case 403 -> PhoneEligibilityBindingFailureCode.HTTP_403;
			case 408 -> PhoneEligibilityBindingFailureCode.HTTP_408;
			case 409 -> PhoneEligibilityBindingFailureCode.HTTP_409;
			case 422 -> PhoneEligibilityBindingFailureCode.HTTP_422;
			case 425 -> PhoneEligibilityBindingFailureCode.HTTP_425;
			case 429 -> PhoneEligibilityBindingFailureCode.HTTP_429;
			default -> status >= 300 && status < 400
					? PhoneEligibilityBindingFailureCode.HTTP_3XX
					: status >= 500 ? PhoneEligibilityBindingFailureCode.HTTP_5XX
					: PhoneEligibilityBindingFailureCode.HTTP_OTHER;
		};
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	private static Duration requirePositive(Duration value, String name) {
		if (value == null || value.isZero() || value.isNegative())
			throw new IllegalArgumentException(name + " must be positive");
		return value;
	}
}
