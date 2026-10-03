package web.tosunsaeng.identity.domain.auth.mergeprogress;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
@Document("merge_query_quotas")
public record MergeQueryQuota(@Id String id, int count,
		@Indexed(name="ttl_merge_quota", expireAfter="0s") Instant cleanupAt) {}
