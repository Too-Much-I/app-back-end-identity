package web.tosunsaeng.identity.domain.auth.application.firebase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.repository.FirebaseEnrollmentAttemptRepository;

public final class FirebaseEnrollmentAttemptService {

	private static final int MAX_CONCURRENCY_RETRIES = 4;

	private final FirebaseEnrollmentAttemptRepository repository;
	private final Clock clock;
	private final Duration enrollmentTtl;
	private final Duration cleanupRetention;

	public FirebaseEnrollmentAttemptService(
			FirebaseEnrollmentAttemptRepository repository,
			Clock clock,
			Duration enrollmentTtl,
			Duration cleanupRetention
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.enrollmentTtl = requirePositive(enrollmentTtl, "enrollmentTtl");
		this.cleanupRetention = requirePositive(cleanupRetention, "cleanupRetention");
	}

	public FirebaseEnrollmentAttempt startOrReuse(
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId,
			FirebaseAuthenticationMethod initialSignInMethod
	) {
		Instant firstAttemptAt = clock.instant();
		FirebaseEnrollmentAttempt candidate = FirebaseEnrollmentAttempt.create(
				firebaseProjectId,
				firebaseUid,
				bindingType,
				boundUserId,
				initialSignInMethod,
				firstAttemptAt,
				enrollmentTtl,
				cleanupRetention
		);

		for (int retry = 0; retry < MAX_CONCURRENCY_RETRIES; retry++) {
			Instant now = clock.instant();
			Optional<FirebaseEnrollmentAttempt> pending = repository.findPendingByBinding(
					candidate.getFirebaseProjectId(),
					candidate.getFirebaseUid(),
					candidate.getBindingType(),
					candidate.getBoundUserId()
			);
			if (pending.isPresent()) {
				FirebaseEnrollmentAttempt existing = pending.orElseThrow();
				if (existing.isActiveAt(now)) {
					return existing;
				}
				repository.expireIfPendingAndExpired(existing.getEnrollmentId(), now);
				continue;
			}

			try {
				return repository.save(candidate);
			} catch (DuplicateKeyException exception) {
				// 동시 insert 승자가 만든 PENDING attempt를 다음 반복에서 재조회한다.
			}
		}
		throw new IllegalStateException("Active Firebase enrollment could not be resolved.");
	}

	public boolean consume(
			String enrollmentId,
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId
	) {
		return repository.consumeIfPendingAndNotExpired(
				Objects.requireNonNull(enrollmentId, "enrollmentId must not be null"),
				Objects.requireNonNull(
						firebaseProjectId,
						"firebaseProjectId must not be null"
				),
				Objects.requireNonNull(firebaseUid, "firebaseUid must not be null"),
				Objects.requireNonNull(bindingType, "bindingType must not be null"),
				boundUserId,
				clock.instant()
		);
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return required;
	}
}
