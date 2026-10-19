package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

@Getter
@Document("provider_unlink_operations")
@CompoundIndex(name = "uk_provider_unlink_request", def = "{'userId':1,'requestIdHash':1}", unique = true)
@CompoundIndex(name = "ix_provider_unlink_due", def = "{'state':1,'nextAttemptAt':1,'leaseUntil':1}")
public class ProviderUnlinkOperation {
	public enum State { PENDING, UNLINK_STARTED, UNLINK_ACKED, REVOKE_STARTED, REVOKE_ACKED,
		COMPLETED, RECONCILIATION_REQUIRED, SUPERSEDED }
	@Id private String operationId;
	@Version private Long version;
	private String userId;
	private String requestIdHash;
	private SocialProvider provider;
	private String bindingId;
	private Instant bindingCreatedAt;
	private String firebaseProjectId;
	private String firebaseUid;
	private String firebaseTenantId;
	private String targetSocialIdentityId;
	private Set<SocialProvider> remainingProviders;
	private Instant acceptedAt;
	private long acceptedEpoch;
	private State state;
	private String leaseOwner;
	private Instant leaseUntil;
	private Instant nextAttemptAt;
	private Instant remoteCreatedAt;
	private Instant dispatchAt;
	private int attemptCount;
	private String failureCode;
	private Instant completedAt;
	@Indexed(name = "ttl_provider_unlink_cleanup", expireAfter = "0s") private Instant cleanupAt;
	private ProviderUnlinkOperation() { }
	public static ProviderUnlinkOperation create(String requestHash, FirebaseIdentity binding, SocialProvider provider,
			String socialId, Set<SocialProvider> remaining, Instant now, long epoch, String tenantId) {
		var op = new ProviderUnlinkOperation();
		op.operationId = UUID.randomUUID().toString(); op.requestIdHash = requestHash;
		op.userId = binding.getUserId(); op.bindingId = binding.getFirebaseIdentityId();
		op.bindingCreatedAt = binding.getCreatedAt(); op.provider = provider; op.targetSocialIdentityId = socialId;
		op.firebaseProjectId = binding.getFirebaseProjectId(); op.firebaseUid = binding.getFirebaseUid(); op.firebaseTenantId = tenantId;
		op.remainingProviders = Set.copyOf(remaining); op.acceptedAt = now; op.acceptedEpoch = epoch;
		op.state = State.PENDING; op.nextAttemptAt = now;
		return op;
	}
	public String slot() { return "unlink:" + operationId; }
	public boolean terminal() { return state == State.COMPLETED || state == State.SUPERSEDED; }
	public boolean inFlight() { return state == State.UNLINK_STARTED || state == State.REVOKE_STARTED; }
	public void claim(String owner, Instant until) { leaseOwner = owner; leaseUntil = until; }
	public void start(State phase, Instant created, Instant now) {
		if (!((state == State.PENDING && phase == State.UNLINK_STARTED)
				|| (state == State.UNLINK_ACKED && phase == State.REVOKE_STARTED))) throw new IllegalStateException("Invalid phase.");
		state = phase; remoteCreatedAt = created; dispatchAt = now;
	}
	public void acknowledge() {
		if (state == State.UNLINK_STARTED) state = State.UNLINK_ACKED;
		else if (state == State.REVOKE_STARTED) state = State.REVOKE_ACKED;
		else throw new IllegalStateException("No dispatched mutation.");
	}
	public void retry(Instant next) { attemptCount++; nextAttemptAt = next; leaseOwner = null; leaseUntil = null; }
	public void unknown(String code) { state = State.RECONCILIATION_REQUIRED; failureCode = code; cleanupAt = null; }
	public void finish(State terminal, Instant now, Instant cleanup) {
		if (terminal != State.COMPLETED && terminal != State.SUPERSEDED) throw new IllegalArgumentException("Invalid terminal.");
		state = terminal; completedAt = now; cleanupAt = cleanup; leaseOwner = null; leaseUntil = null;
	}
	@Override public String toString() { return "ProviderUnlinkOperation[state=" + state + ",provider=" + provider + "]"; }
}
