package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class UserWithdrawalIdentityReleaseTransactionServiceTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant REQUESTED_AT = Instant.parse("2026-08-26T01:00:00Z");
	private static final Instant EXTERNAL_DELETED_AT = REQUESTED_AT.plusSeconds(10);
	private static final Instant RELEASED_AT = REQUESTED_AT.plusSeconds(20);

	private UserWithdrawalLifecycleRepository lifecycleRepository;
	private UserRepository userRepository;
	private FirebaseIdentityRepository firebaseIdentityRepository;
	private SocialIdentityRepository socialIdentityRepository;
	private PhoneIdentityRepository phoneIdentityRepository;
	private PhoneFingerprintAliasRepository aliasRepository;
	private PhoneEligibilityBindingRevisionRepository bindingRevisionRepository;
	private PhoneEligibilityBindingOutboxRepository bindingOutboxRepository;
	private UserWithdrawalIdentityReleaseTransactionService service;
	private UserWithdrawalLifecycle lifecycle;

	@BeforeEach
	void setUp() {
		lifecycleRepository = mock(UserWithdrawalLifecycleRepository.class);
		userRepository = mock(UserRepository.class);
		firebaseIdentityRepository = mock(FirebaseIdentityRepository.class);
		socialIdentityRepository = mock(SocialIdentityRepository.class);
		phoneIdentityRepository = mock(PhoneIdentityRepository.class);
		aliasRepository = mock(PhoneFingerprintAliasRepository.class);
		bindingRevisionRepository = mock(PhoneEligibilityBindingRevisionRepository.class);
		bindingOutboxRepository = mock(PhoneEligibilityBindingOutboxRepository.class);
		service = new UserWithdrawalIdentityReleaseTransactionService(
				lifecycleRepository,
				userRepository,
				firebaseIdentityRepository,
				socialIdentityRepository,
				phoneIdentityRepository,
				aliasRepository,
				bindingRevisionRepository,
				bindingOutboxRepository
		);
		lifecycle = lifecycle("test-project", "opaque-uid");
		when(lifecycleRepository.findById("withdrawal-id")).thenReturn(Optional.of(lifecycle));
		User user = mock(User.class);
		when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(bindingRevisionRepository.findAllByUserIdAndActiveTrue(USER_ID))
				.thenReturn(List.of());
	}

	@Test
	void releasesAllIdentityMappingsAndMarksCleanedInOneTransaction() {
		FirebaseIdentity firebaseIdentity = FirebaseIdentity.create(
				"test-project", "opaque-uid", USER_ID, REQUESTED_AT
		);
		SocialIdentity google = SocialIdentity.create(
				USER_ID, SocialProvider.GOOGLE, "google-subject", REQUESTED_AT
		);
		SocialIdentity apple = SocialIdentity.create(
				USER_ID, SocialProvider.APPLE, "apple-subject", REQUESTED_AT
		);
		PhoneIdentity phoneIdentity = PhoneIdentity.create(
				USER_ID, new PhoneFingerprint("v1", "A".repeat(43)), REQUESTED_AT
		);
		PhoneFingerprintAlias alias = PhoneFingerprintAlias.create(
				phoneIdentity.getPhoneIdentityId(),
				USER_ID,
				new PhoneFingerprint("v1", "A".repeat(43)),
				REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID))
				.thenReturn(Optional.of(firebaseIdentity));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.of(firebaseIdentity));
		when(socialIdentityRepository.findAllByUserId(USER_ID))
				.thenReturn(List.of(google, apple));
		when(phoneIdentityRepository.findByUserIdAndStatus(USER_ID, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.of(phoneIdentity));
		when(aliasRepository.findAllByUserIdAndStatus(
				USER_ID, PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of(alias));
		when(aliasRepository.findAllByPhoneIdentityIdAndStatus(
				phoneIdentity.getPhoneIdentityId(), PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of(alias));
		when(aliasRepository.releaseAllActiveByPhoneIdentityId(
				phoneIdentity.getPhoneIdentityId(), RELEASED_AT
		)).thenReturn(1L);
		when(lifecycleRepository.markIdentityReleaseCleaned(
				"withdrawal-id", 7L, RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.CLEANED);
		verify(firebaseIdentityRepository).deleteById(firebaseIdentity.getFirebaseIdentityId());
		verify(socialIdentityRepository).deleteAll(List.of(google, apple));
		verify(phoneIdentityRepository).save(phoneIdentity);
		assertThat(phoneIdentity.getStatus()).isEqualTo(PhoneIdentityStatus.RELEASED);
		verify(lifecycleRepository).markIdentityReleaseCleaned(
				"withdrawal-id", 7L, RELEASED_AT
		);
	}

	@Test
	void fullyReleasedStateConvergesIdempotentlyToCleaned() {
		when(firebaseIdentityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.empty());
		emptyUserMappings();
		when(lifecycleRepository.markIdentityReleaseCleaned(
				"withdrawal-id", 7L, RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.IDEMPOTENT);
		verify(firebaseIdentityRepository, never()).deleteById(any());
		verify(socialIdentityRepository, never()).deleteAll(any());
	}

	@Test
	void partialFirebaseStateMovesToReconciliationWithoutMutation() {
		FirebaseIdentity firebaseIdentity = FirebaseIdentity.create(
				"test-project", "opaque-uid", USER_ID, REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID))
				.thenReturn(Optional.of(firebaseIdentity));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.empty());
		when(lifecycleRepository.markIdentityReleaseReconciliationRequired(
				"withdrawal-id",
				7L,
				WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PARTIAL_STATE,
				RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.RECONCILIATION_REQUIRED);
		verify(firebaseIdentityRepository, never()).deleteById(any());
		verify(phoneIdentityRepository, never()).save(any());
	}

	@Test
	void localOrGuestLifecycleWithoutFirebaseTargetReleasesRemainingMappings() {
		lifecycle = lifecycle(null, null);
		when(lifecycleRepository.findById("withdrawal-id")).thenReturn(Optional.of(lifecycle));
		SocialIdentity socialIdentity = SocialIdentity.create(
				USER_ID, SocialProvider.KAKAO, "kakao-subject", REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(socialIdentityRepository.findAllByUserId(USER_ID))
				.thenReturn(List.of(socialIdentity));
		when(phoneIdentityRepository.findByUserIdAndStatus(USER_ID, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(aliasRepository.findAllByUserIdAndStatus(
				USER_ID, PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of());
		when(lifecycleRepository.markIdentityReleaseCleaned(
				"withdrawal-id", 7L, RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.CLEANED);
		verify(socialIdentityRepository).deleteAll(List.of(socialIdentity));
		verify(firebaseIdentityRepository, never()).deleteById(any());
	}

	@Test
	void localOrGuestLifecycleWithFirebaseMappingRequiresReconciliation() {
		lifecycle = lifecycle(null, null);
		when(lifecycleRepository.findById("withdrawal-id")).thenReturn(Optional.of(lifecycle));
		FirebaseIdentity unexpected = FirebaseIdentity.create(
				"test-project", "opaque-uid", USER_ID, REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID))
				.thenReturn(Optional.of(unexpected));
		when(lifecycleRepository.markIdentityReleaseReconciliationRequired(
				"withdrawal-id",
				7L,
				WithdrawalCleanupFailureCode.IDENTITY_RELEASE_OWNERSHIP_MISMATCH,
				RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.RECONCILIATION_REQUIRED);
		verify(firebaseIdentityRepository, never()).deleteById(any());
		verify(socialIdentityRepository, never()).deleteAll(any());
	}

	@Test
	void firebaseTargetOwnedByAnotherUserRequiresReconciliation() {
		String otherUserId = "8adfa21e-176f-40ac-aa6c-73996319fa15";
		FirebaseIdentity userIdentity = FirebaseIdentity.create(
				"test-project", "opaque-uid", USER_ID, REQUESTED_AT
		);
		FirebaseIdentity targetIdentity = FirebaseIdentity.create(
				"test-project", "opaque-uid", otherUserId, REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID))
				.thenReturn(Optional.of(userIdentity));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.of(targetIdentity));
		when(lifecycleRepository.markIdentityReleaseReconciliationRequired(
				"withdrawal-id",
				7L,
				WithdrawalCleanupFailureCode.IDENTITY_RELEASE_OWNERSHIP_MISMATCH,
				RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.RECONCILIATION_REQUIRED);
		verify(firebaseIdentityRepository, never()).deleteById(any());
	}

	@Test
	void orphanActivePhoneAliasRequiresReconciliation() {
		PhoneFingerprintAlias orphanAlias = PhoneFingerprintAlias.create(
				"7fe70d6f-0942-4531-9f33-b1638196d9f8",
				USER_ID,
				new PhoneFingerprint("v1", "A".repeat(43)),
				REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.empty());
		when(socialIdentityRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
		when(phoneIdentityRepository.findByUserIdAndStatus(USER_ID, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(aliasRepository.findAllByUserIdAndStatus(
				USER_ID, PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of(orphanAlias));
		when(lifecycleRepository.markIdentityReleaseReconciliationRequired(
				"withdrawal-id",
				7L,
				WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PARTIAL_STATE,
				RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.RECONCILIATION_REQUIRED);
		verify(aliasRepository, never()).releaseAllActiveByPhoneIdentityId(any(), any());
	}

	@Test
	void phoneIdentityAliasOwnedByAnotherUserRequiresReconciliation() {
		String otherUserId = "8adfa21e-176f-40ac-aa6c-73996319fa15";
		PhoneIdentity phoneIdentity = PhoneIdentity.create(
				USER_ID, new PhoneFingerprint("v1", "A".repeat(43)), REQUESTED_AT
		);
		PhoneFingerprintAlias ownedAlias = PhoneFingerprintAlias.create(
				phoneIdentity.getPhoneIdentityId(),
				USER_ID,
				new PhoneFingerprint("v1", "A".repeat(43)),
				REQUESTED_AT
		);
		PhoneFingerprintAlias foreignAlias = PhoneFingerprintAlias.create(
				phoneIdentity.getPhoneIdentityId(),
				otherUserId,
				new PhoneFingerprint("v2", "B".repeat(43)),
				REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.empty());
		when(socialIdentityRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
		when(phoneIdentityRepository.findByUserIdAndStatus(USER_ID, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.of(phoneIdentity));
		when(aliasRepository.findAllByUserIdAndStatus(
				USER_ID, PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of(ownedAlias));
		when(aliasRepository.findAllByPhoneIdentityIdAndStatus(
				phoneIdentity.getPhoneIdentityId(), PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of(ownedAlias, foreignAlias));
		when(lifecycleRepository.markIdentityReleaseReconciliationRequired(
				"withdrawal-id",
				7L,
				WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PARTIAL_STATE,
				RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.RECONCILIATION_REQUIRED);
		verify(aliasRepository, never()).releaseAllActiveByPhoneIdentityId(any(), any());
		verify(phoneIdentityRepository, never()).save(any());
	}

	@Test
	void activeEligibilityBindingIsRevokedAndOutboxedBeforeCleaned() {
		FirebaseIdentity firebaseIdentity = FirebaseIdentity.create(
				"test-project", "opaque-uid", USER_ID, REQUESTED_AT
		);
		PhoneEligibilityBindingRevision active = mock(PhoneEligibilityBindingRevision.class);
		PhoneEligibilityBindingRevision revoked = mock(PhoneEligibilityBindingRevision.class);
		when(active.getUserId()).thenReturn(USER_ID);
		when(active.getConsumerScopeId()).thenReturn("billing-trial-v1");
		when(active.getRevision()).thenReturn(3L);
		when(revoked.getUserId()).thenReturn(USER_ID);
		when(revoked.getConsumerScopeId()).thenReturn("billing-trial-v1");
		when(revoked.getRevision()).thenReturn(4L);
		when(firebaseIdentityRepository.findByUserId(USER_ID))
				.thenReturn(Optional.of(firebaseIdentity));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.of(firebaseIdentity));
		emptyUserMappings();
		when(bindingRevisionRepository.findAllByUserIdAndActiveTrue(USER_ID))
				.thenReturn(List.of(active));
		when(bindingRevisionRepository.advanceRevoked(
				USER_ID, "billing-trial-v1", 3L, RELEASED_AT
		)).thenReturn(Optional.of(revoked));
		when(lifecycleRepository.markIdentityReleaseCleaned(
				"withdrawal-id", 7L, RELEASED_AT
		)).thenReturn(true);

		assertThat(service.release(lifecycle, RELEASED_AT))
				.isEqualTo(IdentityReleaseOutcome.CLEANED);
		verify(bindingRevisionRepository).advanceRevoked(
				USER_ID, "billing-trial-v1", 3L, RELEASED_AT
		);
		verify(bindingOutboxRepository).save(any(PhoneEligibilityBindingOutbox.class));
		verify(lifecycleRepository).markIdentityReleaseCleaned(
				"withdrawal-id", 7L, RELEASED_AT
		);
	}

	@Test
	void lifecycleCasLossThrowsSoTransactionCanRollBackAllMutations() {
		FirebaseIdentity firebaseIdentity = FirebaseIdentity.create(
				"test-project", "opaque-uid", USER_ID, REQUESTED_AT
		);
		when(firebaseIdentityRepository.findByUserId(USER_ID))
				.thenReturn(Optional.of(firebaseIdentity));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "opaque-uid"
		)).thenReturn(Optional.of(firebaseIdentity));
		emptyUserMappings();
		when(lifecycleRepository.markIdentityReleaseCleaned(
				"withdrawal-id", 7L, RELEASED_AT
		)).thenReturn(false);

		assertThatThrownBy(() -> service.release(lifecycle, RELEASED_AT))
				.isInstanceOf(OptimisticLockingFailureException.class);
		verify(firebaseIdentityRepository).deleteById(firebaseIdentity.getFirebaseIdentityId());
	}

	@Test
	void transactionBoundaryUsesNamedMongoManager() throws Exception {
		Method method = UserWithdrawalIdentityReleaseTransactionService.class.getMethod(
				"release",
				UserWithdrawalLifecycle.class,
				Instant.class
		);
		Transactional annotation = method.getAnnotation(Transactional.class);

		assertThat(annotation).isNotNull();
		assertThat(annotation.transactionManager()).isEqualTo("mongoTransactionManager");
	}

	private void emptyUserMappings() {
		when(socialIdentityRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
		when(phoneIdentityRepository.findByUserIdAndStatus(USER_ID, PhoneIdentityStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(aliasRepository.findAllByUserIdAndStatus(
				USER_ID, PhoneFingerprintAliasStatus.ACTIVE
		)).thenReturn(List.of());
	}

	private UserWithdrawalLifecycle lifecycle(String projectId, String firebaseUid) {
		UserWithdrawalLifecycle value = mock(UserWithdrawalLifecycle.class);
		when(value.getWithdrawalId()).thenReturn("withdrawal-id");
		when(value.getUserId()).thenReturn(USER_ID);
		when(value.getStatus()).thenReturn(UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING);
		when(value.getFirebaseProjectId()).thenReturn(projectId);
		when(value.getFirebaseUid()).thenReturn(firebaseUid);
		when(value.getRequestedAt()).thenReturn(REQUESTED_AT);
		when(value.getExternalDeletedAt()).thenReturn(EXTERNAL_DELETED_AT);
		when(value.getVersion()).thenReturn(7L);
		return value;
	}
}
