package web.tosunsaeng.identity.domain.user.application;

public interface FirebaseWithdrawalCleanupPort {

	FirebaseCleanupAccountSnapshot inspect(String projectId, String firebaseUid);

	void disable(String projectId, String firebaseUid);

	void revokeRefreshTokens(String projectId, String firebaseUid);

	ProviderObligationResult satisfyProviderDeletionObligations(
			FirebaseCleanupAccountSnapshot snapshot
	);

	void delete(String projectId, String firebaseUid);

	FirebaseAccountPresence checkPresence(String projectId, String firebaseUid);
}
