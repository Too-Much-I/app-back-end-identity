package web.tosunsaeng.identity.domain.auth.mergeprogress;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Component;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.global.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class MergeQuerySupport {
	private final MongoTemplate mongo;
	public void limit(String userId, Instant now) {
		long window = Math.floorDiv(now.getEpochSecond(), 60);
		var query = Query.query(Criteria.where("_id").is(userId + ":" + window));
		var update = new Update().inc("count", 1).setOnInsert("cleanupAt", Instant.ofEpochSecond((window + 2) * 60));
		MergeQueryQuota quota;
		try {
			quota = mongo.findAndModify(query, update, FindAndModifyOptions.options().upsert(true).returnNew(true), MergeQueryQuota.class);
		} catch (DuplicateKeyException race) {
			quota = mongo.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), MergeQueryQuota.class);
		}
		if (quota == null) throw new AuthException(AuthErrorStatus.MERGE_STATUS_UNAVAILABLE);
		if (quota.count() > 30) throw new RateLimited((window + 1) * 60 - now.getEpochSecond());
	}
	public MergeQueryCursor decode(String token, String userId, boolean activeOnly, Instant now) {
		uuid(token);
		var cursor = mongo.findById(token, MergeQueryCursor.class);
		if (cursor == null || !cursor.userId().equals(userId) || cursor.activeOnly() != activeOnly
				|| !cursor.cleanupAt().isAfter(now)) throw invalid();
		return cursor;
	}
	public String encode(String userId, boolean activeOnly, UserMergeProgress last, Instant now) {
		String token = UUID.randomUUID().toString();
		mongo.insert(new MergeQueryCursor(token, userId, activeOnly, last.getCreatedAt(), last.getMergeId(),
				now.plus(Duration.ofHours(1))));
		return token;
	}
	public static void uuid(String value) {
		try { if (!UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException(); }
		catch (RuntimeException e) { throw invalid(); }
	}
	public static AuthException invalid() { return new AuthException(AuthErrorStatus.INVALID_MERGE_STATUS_REQUEST); }
	public static final class RateLimited extends BusinessException {
		private final long retryAfterSeconds;
		public RateLimited(long seconds) { super(AuthErrorStatus.MERGE_STATUS_RATE_LIMITED); retryAfterSeconds = seconds; }
		public long retryAfterSeconds() { return retryAfterSeconds; }
	}
}
