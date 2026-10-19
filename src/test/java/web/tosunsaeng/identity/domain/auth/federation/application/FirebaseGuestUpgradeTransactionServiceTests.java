package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneIdentityTransactionService;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class FirebaseGuestUpgradeTransactionServiceTests {

	private static final Instant CREATED_AT = Instant.parse("2026-08-18T03:00:00Z");
	private static final Instant NOW = Instant.parse("2026-08-18T04:00:00Z");

	private UserRepository userRepository;
	private FirebaseIdentityRepository firebaseIdentityRepository;
	private SocialIdentityRepository socialIdentityRepository;
	private PhoneIdentityTransactionService phoneTransactionService;
	private RefreshSessionRepository refreshSessionRepository;
	private RefreshSessionIssuer refreshSessionIssuer;
	private FirebaseEnrollmentAttemptRepository enrollmentRepository;
	private RecordingTransactionManager transactionManager;
	private AnnotationConfigApplicationContext context;
	private FirebaseGuestUpgradeTransactionService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		firebaseIdentityRepository = mock(FirebaseIdentityRepository.class);
		socialIdentityRepository = mock(SocialIdentityRepository.class);
		phoneTransactionService = mock(PhoneIdentityTransactionService.class);
		refreshSessionRepository = mock(RefreshSessionRepository.class);
		refreshSessionIssuer = mock(RefreshSessionIssuer.class);
		enrollmentRepository = mock(FirebaseEnrollmentAttemptRepository.class);
		transactionManager = new RecordingTransactionManager();
		when(userRepository.promoteGuestIfUnchanged(any(), any())).thenReturn(true);
		when(enrollmentRepository.consumeIfPendingAndNotExpired(
				any(), any(), any(), any(), any(), any()
		)).thenReturn(true);
		when(refreshSessionIssuer.savePrepared(any())).thenAnswer(invocation -> {
			PreparedRefreshSession prepared = invocation.getArgument(0);
			return new IssuedRefreshSession(
					prepared.tokenValue(),
					prepared.session().getCreatedAt(),
					prepared.session().getExpiresAt()
			);
		});

		context = new AnnotationConfigApplicationContext();
		context.register(TransactionTestConfiguration.class);
		context.registerBean(UserRepository.class, () -> userRepository);
		context.registerBean(FirebaseIdentityRepository.class, () -> firebaseIdentityRepository);
		context.registerBean(SocialIdentityRepository.class, () -> socialIdentityRepository);
		context.registerBean(
				PhoneIdentityTransactionService.class,
				() -> phoneTransactionService
		);
		context.registerBean(RefreshSessionRepository.class, () -> refreshSessionRepository);
		context.registerBean(RefreshSessionIssuer.class, () -> refreshSessionIssuer);
		context.registerBean(FirebaseEnrollmentAttemptRepository.class, () -> enrollmentRepository);
		context.registerBean(
				"mongoTransactionManager",
				PlatformTransactionManager.class,
				() -> transactionManager
		);
		context.registerBean(FirebaseGuestUpgradeTransactionService.class);
		context.refresh();
		service = context.getBean(FirebaseGuestUpgradeTransactionService.class);
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void promotesAggregateRevokesGuestSessionsAndConsumesAttemptInOneTransaction() {
		Aggregate aggregate = aggregate();
		RefreshSession first = session(aggregate.user().getUserId(), "first-hash");
		RefreshSession second = session(aggregate.user().getUserId(), "second-hash");
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(
				aggregate.user().getUserId()
		)).thenReturn(List.of(first, second));

		IssuedRefreshSession result = upgrade(aggregate);

		assertThat(result.tokenValue()).isEqualTo("refresh-secret");
		assertThat(transactionManager.commits).isEqualTo(1);
		assertThat(transactionManager.rollbacks).isZero();
		assertThat(first.getRevocationReason()).isEqualTo(RevocationReason.GUEST_UPGRADED);
		assertThat(second.getRevocationReason()).isEqualTo(RevocationReason.GUEST_UPGRADED);
		InOrder order = inOrder(
				userRepository,
				firebaseIdentityRepository,
				phoneTransactionService,
				socialIdentityRepository,
				refreshSessionRepository,
				refreshSessionIssuer,
				enrollmentRepository
		);
		order.verify(userRepository).promoteGuestIfUnchanged(
				aggregate.user(),
				CREATED_AT
		);
		order.verify(firebaseIdentityRepository).save(aggregate.firebaseIdentity());
		order.verify(phoneTransactionService).linkOrReplace(
				aggregate.user().getUserId(),
				aggregate.fingerprints(),
				aggregate.consumerScopeId(),
				aggregate.eligibilityCandidates(),
				NOW
		);
		order.verify(socialIdentityRepository).saveAll(aggregate.socialIdentities());
		order.verify(refreshSessionRepository)
				.findAllByUserIdAndRevokedAtIsNull(aggregate.user().getUserId());
		order.verify(refreshSessionRepository).saveAll(List.of(first, second));
		order.verify(refreshSessionIssuer).savePrepared(aggregate.preparedSession());
		order.verify(enrollmentRepository).consumeIfPendingAndNotExpired(
				aggregate.attempt().getEnrollmentId(),
				aggregate.attempt().getFirebaseProjectId(),
				aggregate.attempt().getFirebaseUid(),
				FirebaseEnrollmentBindingType.GUEST_USER,
				aggregate.user().getUserId(),
				NOW
		);
	}

	@Test
	void phoneEligibilityFailureRollsBackBeforeSessionsAndEnrollment() {
		doThrow(new DataAccessResourceFailureException("test-only eligibility failure"))
				.when(phoneTransactionService).linkOrReplace(any(), any(), any(), any(), any());

		Throwable failure = catchThrowable(() -> upgrade(aggregate()));

		assertThat(failure).isInstanceOf(DataAccessResourceFailureException.class);
		assertThat(transactionManager.commits).isZero();
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		verify(refreshSessionRepository, never()).saveAll(any());
		verify(refreshSessionIssuer, never()).savePrepared(any());
		verify(enrollmentRepository, never()).consumeIfPendingAndNotExpired(
				any(), any(), any(), any(), any(), any()
		);
	}

	@Test
	void failedEnrollmentConsumeRollsBackAfterNewSessionSave() {
		Aggregate aggregate = aggregate();
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(any()))
				.thenReturn(List.of());
		when(enrollmentRepository.consumeIfPendingAndNotExpired(
				any(), any(), any(), any(), any(), any()
		)).thenReturn(false);

		Throwable failure = catchThrowable(() -> upgrade(aggregate));

		assertThat(failure)
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT));
		assertThat(transactionManager.commits).isZero();
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		verify(refreshSessionIssuer).savePrepared(aggregate.preparedSession());
	}

	@Test
	void failedGuestCompareAndSetMutatesNoIdentityOrSession() {
		when(userRepository.promoteGuestIfUnchanged(any(), any())).thenReturn(false);

		Throwable failure = catchThrowable(() -> upgrade(aggregate()));

		assertThat(failure)
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT));
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		verify(firebaseIdentityRepository, never()).save(any());
		verify(phoneTransactionService, never()).linkOrReplace(any(), any(), any(), any(), any());
		verify(refreshSessionIssuer, never()).savePrepared(any());
	}

	private IssuedRefreshSession upgrade(Aggregate aggregate) {
		return service.upgrade(
				aggregate.user(),
				CREATED_AT,
				aggregate.firebaseIdentity(),
				aggregate.fingerprints(),
				aggregate.consumerScopeId(),
				aggregate.eligibilityCandidates(),
				aggregate.socialIdentities(),
				aggregate.preparedSession(),
				aggregate.attempt(),
				NOW
		);
	}

	private Aggregate aggregate() {
		User user = User.createGuest(
				"A".repeat(43),
				"게스트",
				UserConsents.consented("privacy-v1", "term-v1", CREATED_AT),
				CREATED_AT
		);
		user.promoteGuestToFederatedMember("승격회원", "privacy-v1", "term-v1", NOW);
		FirebaseIdentity firebaseIdentity = FirebaseIdentity.create(
				"test-project",
				"firebase-uid-sensitive",
				user.getUserId(),
				NOW
		);
		PhoneFingerprint fingerprint = new PhoneFingerprint("v1", "A".repeat(43));
		PhoneFingerprintSet fingerprints = new PhoneFingerprintSet(
				fingerprint,
				List.of(fingerprint)
		);
		List<PhoneEligibilityFingerprintCandidate> eligibilityCandidates = List.of(
				new PhoneEligibilityFingerprintCandidate("v1", "B".repeat(43))
		);
		List<SocialIdentity> socialIdentities = List.of(SocialIdentity.create(
				user.getUserId(),
				SocialProvider.GOOGLE,
				"google-subject-sensitive",
				NOW
		));
		PreparedRefreshSession preparedSession = new PreparedRefreshSession(
				"refresh-secret",
				session(user.getUserId(), "new-member-hash")
		);
		FirebaseEnrollmentAttempt attempt = FirebaseEnrollmentAttempt.create(
				"test-project",
				"firebase-uid-sensitive",
				FirebaseEnrollmentBindingType.GUEST_USER,
				user.getUserId(),
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
		return new Aggregate(
				user,
				firebaseIdentity,
				fingerprints,
				"scope-v1",
				eligibilityCandidates,
				socialIdentities,
				preparedSession,
				attempt
		);
	}

	private RefreshSession session(String userId, String tokenHash) {
		return RefreshSession.create(
				userId,
				tokenHash,
				NOW,
				NOW.plus(Duration.ofDays(14))
		);
	}

	private record Aggregate(
			User user,
			FirebaseIdentity firebaseIdentity,
			PhoneFingerprintSet fingerprints,
			String consumerScopeId,
			List<PhoneEligibilityFingerprintCandidate> eligibilityCandidates,
			List<SocialIdentity> socialIdentities,
			PreparedRefreshSession preparedSession,
			FirebaseEnrollmentAttempt attempt
	) {
	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class TransactionTestConfiguration {
	}

	static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

		private int commits;
		private int rollbacks;
		private final Object transaction = new Object();
		private boolean active;

		@Override
		protected Object doGetTransaction() {
			return transaction;
		}

		@Override
		protected boolean isExistingTransaction(Object transaction) {
			return active;
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
			active = true;
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			commits++;
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
			rollbacks++;
		}

		@Override
		protected void doSetRollbackOnly(DefaultTransactionStatus status) {
			// 참여 중인 inner service 실패는 outer boundary까지 전파되어 함께 rollback된다.
		}

		@Override
		protected void doCleanupAfterCompletion(Object transaction) {
			active = false;
		}
	}
}
