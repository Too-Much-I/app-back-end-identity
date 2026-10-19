package web.tosunsaeng.identity.domain.auth.mergeprogress;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
@Document("merge_query_cursors")
public record MergeQueryCursor(@Id String id, String userId, boolean activeOnly, Instant createdAt, String mergeId,
		@Indexed(name="ttl_merge_cursor", expireAfter="0s") Instant cleanupAt) {}
