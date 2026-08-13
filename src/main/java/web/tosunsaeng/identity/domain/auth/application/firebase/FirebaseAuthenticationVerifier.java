package web.tosunsaeng.identity.domain.auth.application.firebase;

public interface FirebaseAuthenticationVerifier {

	VerifiedFirebasePrincipal verify(
			String firebaseIdToken,
			FirebaseVerificationPurpose purpose
	);
}
