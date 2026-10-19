package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

interface FirebaseAdminClient {

	FirebaseAdminPrincipalData verify(String firebaseIdToken, boolean checkRevoked);
}
