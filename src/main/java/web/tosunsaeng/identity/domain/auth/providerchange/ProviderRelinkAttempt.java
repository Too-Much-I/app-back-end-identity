package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

@Getter
@Document("provider_relink_attempts")
public class ProviderRelinkAttempt {
	@Id private String attemptId;
	@Version private Long version;
	@Indexed private String userId;
	private String bindingId;
	private SocialProvider provider;
	private long changeRevision;
	private Instant preparedAt;
	private Instant expiresAt;
	private Instant consumedAt;
	@Indexed(name = "ttl_provider_relink_cleanup", expireAfter = "0s") private Instant cleanupAt;
	private ProviderRelinkAttempt() { }
	public static ProviderRelinkAttempt create(String userId, String binding, SocialProvider provider,
			long revision, Instant now, Instant until) {
		var attempt = new ProviderRelinkAttempt(); attempt.attemptId = UUID.randomUUID().toString();
		attempt.userId = userId; attempt.bindingId = binding; attempt.provider = provider;
		attempt.changeRevision = revision; attempt.preparedAt = now; attempt.expiresAt = until;
		return attempt;
	}
	public String slot() { return "relink:" + attemptId; }
	public void consume(Instant now, Instant cleanup) { consumedAt = now; cleanupAt = cleanup; }
	@Override public String toString() { return "ProviderRelinkAttempt[provider=" + provider + ",consumed=" + (consumedAt != null) + "]"; }
}
