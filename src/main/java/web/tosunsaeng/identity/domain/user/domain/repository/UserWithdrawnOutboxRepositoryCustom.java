package web.tosunsaeng.identity.domain.user.domain.repository;

import java.time.Instant;
import java.util.Optional;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnFailureCode;

public interface UserWithdrawnOutboxRepositoryCustom {

	Optional<UserWithdrawnOutbox> claimNext(
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
			UserWithdrawnFailureCode failureCode,
			Instant nextAttemptAt
	);

	boolean markDeadLetter(
			String eventId,
			String leaseOwner,
			UserWithdrawnFailureCode failureCode,
			Instant deadLetteredAt,
			Instant retentionReviewAt
	);

	boolean replayDeadLetter(String eventId, Instant replayAt);
}
