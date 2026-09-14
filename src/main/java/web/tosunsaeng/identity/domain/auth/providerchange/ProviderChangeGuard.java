package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationMethod;

/** Always installed: disabling new capture must never disable existing security state. */
public class ProviderChangeGuard {
	private ProviderChangeMetrics metrics;
	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setMetrics(ProviderChangeMetrics metrics) { this.metrics = metrics; }
	private final MongoTemplate mongo;
	public ProviderChangeGuard(MongoTemplate mongo) { this.mongo = mongo; }
	public boolean hasState(String userId) { return mongo.exists(Query.query(Criteria.where("_id").is(userId)), AuthMethodChangeControl.class); }
	public void validatePrincipal(web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal principal) {
		var binding = mongo.findOne(Query.query(Criteria.where("firebaseProjectId").is(principal.firebaseProjectId())
				.and("firebaseUid").is(principal.firebaseUid())), web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity.class);
		if (binding != null) {
			authenticate(binding.getUserId(), binding.getFirebaseIdentityId(), principal.signInMethod(), principal.authTime());
			SocialProvider provider = social(principal.signInMethod());
			if (provider != null) {
				var target = principal.linkedSocialPrincipals().stream().filter(p -> p.provider() == provider).findFirst()
						.orElseThrow(() -> error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED));
				if (!mongo.exists(Query.query(Criteria.where("userId").is(binding.getUserId()).and("provider").is(provider)
						.and("providerSubject").is(target.providerSubject())), web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity.class)) {
					throw error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
				}
			}
		}
	}
	public AuthMethodChangeControl control(String userId, String bindingId) {
		var control = mongo.findById(userId, AuthMethodChangeControl.class);
		if (control == null) return new AuthMethodChangeControl(userId, bindingId);
		if (!Objects.equals(bindingId, control.getBindingId())) throw error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		return control;
	}
	public void authenticate(String userId, String bindingId, FirebaseAuthenticationMethod method, Instant authTime) {
		var control = mongo.findById(userId, AuthMethodChangeControl.class);
		if (control == null) return;
		if (!Objects.equals(bindingId, control.getBindingId())) throw error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		if (method == null) throw error(AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED);
		SocialProvider provider = social(method);
		if (provider == null) return;
		if (control.isBlocked(provider)) {
			if (metrics != null) metrics.rejected(ProviderChangeMetrics.Outcome.BLOCKED_LOGIN);
			throw error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
		}
		Instant floor = control.getAuthenticationFloors().get(provider.name());
		if (floor != null && (authTime == null || !authTime.isAfter(floor))) throw error(AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED);
	}
	public boolean unresolved(String userId) {
		return mongo.exists(Query.query(Criteria.where("userId").is(userId).and("state").nin(
				ProviderUnlinkOperation.State.COMPLETED, ProviderUnlinkOperation.State.SUPERSEDED)), ProviderUnlinkOperation.class)
				|| mongo.exists(Query.query(Criteria.where("userId").is(userId).and("consumedAt").is(null)), ProviderRelinkAttempt.class)
				|| mongo.exists(Query.query(Criteria.where("userId").is(userId).and("state").is(ProviderLinkAttempt.State.STARTED)), ProviderLinkAttempt.class);
	}
	public static SocialProvider social(FirebaseAuthenticationMethod method) {
		if (method == null) return null;
		return switch (method) { case GOOGLE -> SocialProvider.GOOGLE; case APPLE -> SocialProvider.APPLE;
			case KAKAO -> SocialProvider.KAKAO; default -> null; };
	}
	public static AuthException error(AuthErrorStatus status) { return new AuthException(status); }
}
