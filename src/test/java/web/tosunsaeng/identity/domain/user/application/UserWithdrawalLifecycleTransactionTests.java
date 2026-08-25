package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class UserWithdrawalLifecycleTransactionTests {

	private static final Instant NOW = Instant.parse("2026-08-25T01:02:03Z");

	@Test
	void storesTombstoneSessionsEligibilityAndFirebaseTargetLifecycleTogether() {
		User user = User.createFederatedMember(
				"SNS회원", UserConsents.consented("privacy-v1", "term-v1", NOW), NOW
		);
		FirebaseWithdrawalTarget target = new FirebaseWithdrawalTarget(
				"firebase-project", "firebase-uid"
		);
		String credentialHash = "credential-hash";
		RefreshSession session = RefreshSession.create(
				user.getUserId(), credentialHash, NOW, NOW.plusSeconds(600)
		);
		UserRepository userRepository = mock(UserRepository.class);
		RefreshSessionRepository sessionRepository = mock(RefreshSessionRepository.class);
		PhoneEligibilityBindingRevisionRepository revisionRepository = mock(
				PhoneEligibilityBindingRevisionRepository.class
		);
		PhoneEligibilityBindingOutboxRepository outboxRepository = mock(
				PhoneEligibilityBindingOutboxRepository.class
		);
		UserWithdrawalLifecycleRepository lifecycleRepository = mock(
				UserWithdrawalLifecycleRepository.class
		);
		FirebaseIdentityRepository identityRepository = mock(FirebaseIdentityRepository.class);
		when(sessionRepository.findByTokenHash(credentialHash)).thenReturn(Optional.of(session));
		when(userRepository.withdrawIfUnchanged(any(), any(), any())).thenReturn(true);
		when(identityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"firebase-project", "firebase-uid"
		)).thenReturn(Optional.of(FirebaseIdentity.create(
				"firebase-project", "firebase-uid", user.getUserId(), NOW
		)));
		when(lifecycleRepository.findByUserId(user.getUserId())).thenReturn(Optional.empty());
		when(revisionRepository.findAllByUserIdAndActiveTrue(user.getUserId()))
				.thenReturn(List.of());
		when(sessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getUserId()))
				.thenReturn(List.of(session));
		UserWithdrawalTransactionService service = new UserWithdrawalTransactionService(
				userRepository, sessionRepository, revisionRepository, outboxRepository,
				lifecycleRepository, identityRepository
		);

		WithdrawalTransactionResult result = service.withdraw(
				user, credentialHash, NOW, target
		);

		ArgumentCaptor<UserWithdrawalLifecycle> lifecycleCaptor = ArgumentCaptor.forClass(
				UserWithdrawalLifecycle.class
		);
		verify(lifecycleRepository).save(lifecycleCaptor.capture());
		UserWithdrawalLifecycle lifecycle = lifecycleCaptor.getValue();
		assertThat(lifecycle.getUserId()).isEqualTo(user.getUserId());
		assertThat(lifecycle.getFirebaseProjectId()).isEqualTo("firebase-project");
		assertThat(lifecycle.getFirebaseUid()).isEqualTo("firebase-uid");
		assertThat(lifecycle.getStatus())
				.isEqualTo(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_PENDING);
		assertThat(result.response().cleanupStatus())
				.isEqualTo(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_PENDING);
		assertThat(session.isRevoked()).isTrue();
	}
}
