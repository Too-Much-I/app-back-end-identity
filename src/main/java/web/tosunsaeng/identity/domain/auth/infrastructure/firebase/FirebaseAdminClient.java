package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

interface FirebaseAdminClient {

	FirebaseAdminPrincipalData verify(String firebaseIdToken, boolean checkRevoked);
}
