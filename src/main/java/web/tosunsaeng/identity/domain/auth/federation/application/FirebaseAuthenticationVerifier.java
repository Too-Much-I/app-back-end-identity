package web.tosunsaeng.identity.domain.auth.federation.application;

public interface FirebaseAuthenticationVerifier {

	VerifiedFirebasePrincipal verify(
			String firebaseIdToken,
			FirebaseVerificationPurpose purpose
	);
}
