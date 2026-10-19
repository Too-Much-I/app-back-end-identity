package web.tosunsaeng.identity.domain.user.application;

import java.util.Objects;

public record FirebaseWithdrawalTarget(String firebaseProjectId, String firebaseUid) {
	public FirebaseWithdrawalTarget {
		firebaseProjectId = Objects.requireNonNull(firebaseProjectId);
		firebaseUid = Objects.requireNonNull(firebaseUid);
	}
}
