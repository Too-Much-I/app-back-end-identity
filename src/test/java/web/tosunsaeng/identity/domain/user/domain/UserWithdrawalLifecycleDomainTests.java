package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;

class UserWithdrawalLifecycleDomainTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant NOW = Instant.parse("2026-08-25T01:02:03Z");

	@Test
	void createsPendingLifecycleWithoutPersistingCredentialMaterial() {
		UserWithdrawalLifecycle lifecycle = UserWithdrawalLifecycle.create(
				USER_ID, "firebase-project", "opaque-uid", NOW
		);

		assertThat(lifecycle.getWithdrawalId()).isNotBlank();
		assertThat(lifecycle.getUserId()).isEqualTo(USER_ID);
		assertThat(lifecycle.getStatus())
				.isEqualTo(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_PENDING);
		assertThat(lifecycle.getAttemptCount()).isZero();
		assertThat(lifecycle.getRequestedAt()).isEqualTo(NOW);
		assertThat(lifecycle.getUpdatedAt()).isEqualTo(NOW);
		assertThat(lifecycle.getNextAttemptAt()).isEqualTo(NOW);
		assertThat(lifecycle.getLeaseOwner()).isNull();
		assertThat(lifecycle.getLastErrorCode()).isNull();
		assertThat(lifecycle.hasSameTarget("firebase-project", "opaque-uid")).isTrue();
	}

	@Test
	void requiresCompleteFirebaseTargetOrNoTarget() {
		assertThatThrownBy(() -> UserWithdrawalLifecycle.create(
				USER_ID, "firebase-project", null, NOW
		)).isInstanceOf(IllegalArgumentException.class);
		assertThat(UserWithdrawalLifecycle.create(USER_ID, null, null, NOW)
				.hasSameTarget(null, null)).isTrue();
	}

	@Test
	void declaresUniqueUserAndWorkerClaimIndexes() throws Exception {
		Document document = UserWithdrawalLifecycle.class.getAnnotation(Document.class);
		CompoundIndex worker = UserWithdrawalLifecycle.class.getAnnotation(CompoundIndex.class);
		Indexed user = UserWithdrawalLifecycle.class.getDeclaredField("userId")
				.getAnnotation(Indexed.class);

		assertThat(document.collection()).isEqualTo("user_withdrawal_lifecycles");
		assertThat(user.name()).isEqualTo("uk_withdrawal_lifecycle_user_id");
		assertThat(user.unique()).isTrue();
		assertThat(worker.name()).isEqualTo("ix_withdrawal_lifecycle_worker_claim");
		assertThat(worker.def()).contains("status", "nextAttemptAt", "leaseUntil");
	}

	@Test
	void fixesAllowedWorkerAndReleaseTransitions() {
		assertThat(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_PENDING
				.canTransitionTo(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS)).isTrue();
		assertThat(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS
				.canTransitionTo(UserWithdrawalCleanupStatus.RECONCILIATION_REQUIRED)).isTrue();
		assertThat(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_COMPLETED
				.canTransitionTo(UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING)).isTrue();
		assertThat(UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
				.canTransitionTo(UserWithdrawalCleanupStatus.CLEANED)).isTrue();
		assertThat(UserWithdrawalCleanupStatus.CLEANED
				.canTransitionTo(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_PENDING)).isFalse();
	}
}
