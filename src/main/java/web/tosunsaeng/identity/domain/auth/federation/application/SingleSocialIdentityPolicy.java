package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.List;
import java.util.Objects;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

/** Identity approvals, not the Firebase linked-provider list, grant login authority. */
public final class SingleSocialIdentityPolicy {

	private SingleSocialIdentityPolicy() { }

	/** Use the known index name only; database diagnostics must never be returned or logged. */
	public static boolean isUserIndexConflict(org.springframework.dao.DuplicateKeyException exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage();
			if (message != null && message.matches("(?s).*index: uk_social_identities_user_id(?:\\s.*|$)")) return true;
		}
		return false;
	}

	public static SocialProvider social(FirebaseAuthenticationMethod method) {
		return switch (method) {
			case GOOGLE -> SocialProvider.GOOGLE;
			case APPLE -> SocialProvider.APPLE;
			case KAKAO -> SocialProvider.KAKAO;
			default -> null;
		};
	}

	public static void enrollment(VerifiedFirebasePrincipal proof) {
		int count = proof.linkedSocialPrincipals().size();
		if (count > 1) throw new AuthException(AuthErrorStatus.SINGLE_SNS_REQUIRED);
		if (count == 0 && !proof.linkedMethods().contains(FirebaseAuthenticationMethod.PASSWORD)) {
			throw new AuthException(AuthErrorStatus.SINGLE_SNS_REQUIRED);
		}
	}

	public static VerifiedSocialPrincipal current(VerifiedFirebasePrincipal proof) {
		SocialProvider provider = social(proof.signInMethod());
		var candidates = proof.linkedSocialPrincipals().stream().filter(p -> p.provider() == provider).toList();
		if (provider == null || candidates.size() != 1) throw mismatch();
		return candidates.getFirst();
	}

	public static void owned(VerifiedFirebasePrincipal proof, String userId, List<SocialIdentity> identities) {
		// Existing PASSWORD-only accounts retain their login path; PHONE never grants member login.
		if (identities.isEmpty() && proof.signInMethod() == FirebaseAuthenticationMethod.PASSWORD
				&& proof.linkedSocialPrincipals().isEmpty()) return;
		var current = current(proof);
		if (identities.size() != 1) throw mismatch();
		SocialIdentity identity = identities.getFirst();
		if (!Objects.equals(userId, identity.getUserId()) || identity.getProvider() != current.provider()
				|| !identity.getProviderSubject().equals(current.providerSubject())) throw mismatch();
	}

	private static AuthException mismatch() { return new AuthException(AuthErrorStatus.SNS_ACCOUNT_MISMATCH); }
}
