package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

/** New protocol only. Legacy relink attempts cannot safely be treated as unstarted. */
@Getter
@Document("provider_link_attempts")
@CompoundIndex(name = "uk_provider_link_request", def = "{'userId':1,'requestIdHash':1}", unique = true)
public class ProviderLinkAttempt {
	public enum State { PREPARED, STARTED, COMPLETED, ALREADY_LINKED }
	@Id private String attemptId;
	@Version private Long version;
	@Indexed private String userId;
	private String requestIdHash;
	private String bindingId;
	private Instant bindingCreatedAt;
	private String projectId;
	private String firebaseUid;
	private SocialProvider provider;
	private long epoch;
	private long revision;
	private State state;
	private Instant preparedAt;
	private Instant expiresAt;
	private Instant startedAt;
	private Instant completedAt;
	private String socialIdentityId;
	@Indexed(name = "ttl_provider_link_cleanup", expireAfter = "0s") private Instant cleanupAt;
	private ProviderLinkAttempt() { }
	static ProviderLinkAttempt create(FirebaseIdentity binding, SocialProvider provider, String hash,
			long epoch, long revision, Instant now, Instant expires, Instant cleanup) {
		var a = new ProviderLinkAttempt();
		a.attemptId = UUID.randomUUID().toString(); a.userId = binding.getUserId(); a.requestIdHash = hash;
		a.bindingId = binding.getFirebaseIdentityId(); a.bindingCreatedAt = binding.getCreatedAt();
		a.projectId = binding.getFirebaseProjectId(); a.firebaseUid = binding.getFirebaseUid();
		a.provider = provider; a.epoch = epoch; a.revision = revision;
		a.preparedAt = now; a.expiresAt = expires; a.cleanupAt = cleanup; a.state = State.PREPARED;
		return a;
	}
	void start(Instant now, long revision, Instant expires) {
		if (state != State.PREPARED) ProviderChangeService.conflict();
		state = State.STARTED; startedAt = now; this.revision = revision; expiresAt = expires; cleanupAt = null;
	}
	void alreadyLinked(Instant now, String identityId, Instant cleanup) {
		if (state != State.PREPARED) ProviderChangeService.conflict();
		state = State.ALREADY_LINKED; completedAt = now; socialIdentityId = identityId; cleanupAt = cleanup;
	}
	void complete(Instant now, String identityId, Instant cleanup) {
		if (state != State.STARTED) ProviderChangeService.conflict();
		state = State.COMPLETED; completedAt = now; socialIdentityId = identityId; cleanupAt = cleanup;
	}
	String slot() { return "link:" + attemptId; }
	@Override public String toString() { return "ProviderLinkAttempt[provider=" + provider + ",state=" + state + "]"; }
}
