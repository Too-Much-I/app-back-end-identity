package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.common.exception.*;

public final class MongoRecoveryStore implements RecoveryStore {
	@Document("account_recovery_attempts")
	public record Attempt(@Id String id, Instant createdAt, Instant expiresAt, List<String> proofIds,
			Instant retryUntil, @Indexed(expireAfter = "0s") Instant cleanupAt) {
		@Override public String toString() { return "RecoveryAttempt[REDACTED]"; }
	}
	@Document("account_recovery_proofs")
	public record Proof(@Id String id, String recoveryId, @Indexed(expireAfter = "0s") Instant cleanupAt) {
		@Override public String toString() { return "RecoveryProof[REDACTED]"; }
	}
	@Document("account_recovery_budgets")
	public record Budget(@Id String id, long count, @Indexed(expireAfter = "0s") Instant cleanupAt) {
		@Override public String toString() { return "RecoveryBudget[REDACTED]"; }
	}
	private final MongoTemplate mongo;
	private final TransactionTemplate tx;
	public MongoRecoveryStore(MongoTemplate mongo, TransactionTemplate tx) { this.mongo = mongo; this.tx = tx; }
	public void prepare(String id, Instant now, Instant expiresAt) {
		mongo.insert(new Attempt(id, now, expiresAt, null, null, expiresAt.plus(Duration.ofDays(1))));
	}
	public void admit(List<String> keys, int windowSeconds, int limit, Instant now) {
		long window = now.getEpochSecond() / windowSeconds;
		for (String key : keys) {
			Query q = Query.query(Criteria.where("_id").is(key + ":" + window).and("count").lt(limit));
			try {
				mongo.findAndModify(q, new Update().inc("count", 1)
						.setOnInsert("cleanupAt", Instant.ofEpochSecond((window + 1) * windowSeconds).plusSeconds(60)),
						FindAndModifyOptions.options().upsert(true).returnNew(true), Budget.class);
			} catch (DuplicateKeyException e) {
				// ceil(resetAt - now): epoch-second subtraction also rounds fractional seconds up.
				long remainingSeconds = (window + 1) * windowSeconds - now.getEpochSecond();
				throw new RecoveryRateLimitException(remainingSeconds);
			}
		}
	}
	public RecoveryResult lookup(String id, List<String> proofIds, Instant authTime, Instant now,
			Duration retryTtl, Supplier<RecoveryResult> resolve) {
		try {
			return tx.execute(status -> {
				Attempt attempt = mongo.findById(id, Attempt.class);
				if (attempt == null) throw new AuthException(AuthErrorStatus.INVALID_RECOVERY_REQUEST);
				if (authTime.isBefore(attempt.createdAt().truncatedTo(ChronoUnit.SECONDS))) {
					throw new AuthException(AuthErrorStatus.RECOVERY_RECENT_AUTH_REQUIRED);
				}
				if (attempt.proofIds() != null) {
					if (!now.isBefore(attempt.retryUntil())) throw new AuthException(AuthErrorStatus.RECOVERY_EXPIRED);
					if (Collections.disjoint(attempt.proofIds(), proofIds)) throw new AuthException(AuthErrorStatus.RECOVERY_CONFLICT);
				} else {
					if (!now.isBefore(attempt.expiresAt())) throw new AuthException(AuthErrorStatus.RECOVERY_EXPIRED);
					for (String proof : proofIds) mongo.insert(new Proof(proof, id, now.plus(Duration.ofDays(1))));
					var updated = mongo.updateFirst(Query.query(Criteria.where("_id").is(id).and("proofIds").is(null)),
							new Update().set("proofIds", proofIds).set("retryUntil", now.plus(retryTtl))
									.set("cleanupAt", now.plus(retryTtl).plus(Duration.ofDays(1))), Attempt.class);
					if (updated.getModifiedCount() != 1) throw new AuthException(AuthErrorStatus.RECOVERY_CONFLICT);
				}
				// Re-read current ownership on every retry; never cache an old account hint after withdrawal.
				return resolve.get();
			});
		} catch (DuplicateKeyException e) { throw new AuthException(AuthErrorStatus.RECOVERY_CONFLICT); }
	}
}
