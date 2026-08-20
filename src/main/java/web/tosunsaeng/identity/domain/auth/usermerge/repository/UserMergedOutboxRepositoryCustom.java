package web.tosunsaeng.identity.domain.auth.usermerge.repository;

import java.time.Instant;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedFailureCode;

public interface UserMergedOutboxRepositoryCustom {

	Optional<UserMergedOutbox> claimNext(
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
			UserMergedFailureCode failureCode,
			Instant nextAttemptAt
	);

	boolean markDeadLetter(
			String eventId,
			String leaseOwner,
			UserMergedFailureCode failureCode,
			Instant deadLetteredAt,
			Instant retentionReviewAt
	);

	boolean replayDeadLetter(String eventId, Instant replayAt);
}
