package web.tosunsaeng.identity.domain.auth.mergeprogress;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;

/** Retain only unfinished receipts needed by the publisher; expire them immediately after ACK. */
@Service
@RequiredArgsConstructor
public class MergeProgressPrivacyCleanup {
	private final MongoTemplate mongo;
	public void release(String targetUserId, Instant now) {
		mongo.updateMulti(Query.query(Criteria.where("targetUserId").is(targetUserId)),
				new Update().set("privacyCleanupRequested", true).inc("version", 1), UserMergeProgress.class);
		mongo.updateMulti(Query.query(Criteria.where("targetUserId").is(targetUserId).and("completedAt").ne(null)),
				new Update().set("cleanupAt", now).inc("version", 1), UserMergeProgress.class);
		mongo.remove(Query.query(Criteria.where("userId").is(targetUserId)), MergeQueryCursor.class);
	}
}
