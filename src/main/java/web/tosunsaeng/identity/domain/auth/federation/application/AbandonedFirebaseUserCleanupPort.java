package web.tosunsaeng.identity.domain.auth.federation.application;

public interface AbandonedFirebaseUserCleanupPort {

	AbandonedFirebaseAccountSnapshot inspect(String projectId, String firebaseUid);

	void disable(String projectId, String firebaseUid);

	void revokeRefreshTokens(String projectId, String firebaseUid);

	void satisfyProviderDeletionObligations(AbandonedFirebaseAccountSnapshot snapshot);

	void delete(String projectId, String firebaseUid);

	AbandonedFirebaseAccountPresence checkPresence(String projectId, String firebaseUid);
}
