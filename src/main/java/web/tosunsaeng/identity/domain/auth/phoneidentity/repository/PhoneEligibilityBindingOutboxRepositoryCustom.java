package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.time.Instant;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingFailureCode;

public interface PhoneEligibilityBindingOutboxRepositoryCustom {

	Optional<PhoneEligibilityBindingOutbox> claimNext(
			String consumerScopeId,
			String leaseOwner,
			Instant claimedAt,
			Instant leaseExpiresAt
	);

	boolean markPublished(
			String eventId,
			String leaseOwner,
			Instant publishedAt,
			Instant cleanupAt
	);

	boolean scheduleRetry(
			String eventId,
			String leaseOwner,
			PhoneEligibilityBindingFailureCode failureCode,
			Instant nextAttemptAt
	);

	boolean markDeadLetter(
			String eventId,
			String leaseOwner,
			PhoneEligibilityBindingFailureCode failureCode,
			Instant deadLetteredAt,
			Instant retentionReviewAt
	);

	boolean replayDeadLetter(String eventId, Instant replayAt);
}
