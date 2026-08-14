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
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class FirebaseSignupTransactionServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-14T05:00:00Z");

	private UserRepository userRepository;
	private FirebaseIdentityRepository firebaseIdentityRepository;
	private PhoneIdentityRepository phoneIdentityRepository;
	private PhoneFingerprintAliasRepository aliasRepository;
	private PhoneEligibilityBindingOutboxRepository outboxRepository;
	private SocialIdentityRepository socialIdentityRepository;
	private RefreshSessionIssuer refreshSessionIssuer;
	private FirebaseEnrollmentAttemptRepository enrollmentRepository;
	private RecordingTransactionManager transactionManager;
	private AnnotationConfigApplicationContext context;
	private FirebaseSignupTransactionService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		firebaseIdentityRepository = mock(FirebaseIdentityRepository.class);
		phoneIdentityRepository = mock(PhoneIdentityRepository.class);
		aliasRepository = mock(PhoneFingerprintAliasRepository.class);
		outboxRepository = mock(PhoneEligibilityBindingOutboxRepository.class);
		socialIdentityRepository = mock(SocialIdentityRepository.class);
		refreshSessionIssuer = mock(RefreshSessionIssuer.class);
		enrollmentRepository = mock(FirebaseEnrollmentAttemptRepository.class);
		transactionManager = new RecordingTransactionManager();

		when(aliasRepository.findAllActiveByFingerprints(any())).thenReturn(List.of());
		when(phoneIdentityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(refreshSessionIssuer.savePrepared(any())).thenAnswer(invocation -> {
			PreparedRefreshSession prepared = invocation.getArgument(0);
			return new IssuedRefreshSession(
					prepared.tokenValue(),
					prepared.session().getCreatedAt(),
					prepared.session().getExpiresAt()
			);
		});
		when(enrollmentRepository.consumeIfPendingAndNotExpired(
				any(), any(), any(), any(), any(), any()
		)).thenReturn(true);

		context = new AnnotationConfigApplicationContext();
		context.register(TransactionTestConfiguration.class);
		context.registerBean(UserRepository.class, () -> userRepository);
		context.registerBean(FirebaseIdentityRepository.class, () -> firebaseIdentityRepository);
		context.registerBean(PhoneIdentityRepository.class, () -> phoneIdentityRepository);
		context.registerBean(PhoneFingerprintAliasRepository.class, () -> aliasRepository);
		context.registerBean(PhoneEligibilityBindingOutboxRepository.class, () -> outboxRepository);
		context.registerBean(SocialIdentityRepository.class, () -> socialIdentityRepository);
		context.registerBean(RefreshSessionIssuer.class, () -> refreshSessionIssuer);
		context.registerBean(FirebaseEnrollmentAttemptRepository.class, () -> enrollmentRepository);
		context.registerBean(
				"mongoTransactionManager",
				PlatformTransactionManager.class,
				() -> transactionManager
		);
		context.registerBean(FirebaseSignupTransactionService.class);
		context.refresh();
		service = context.getBean(FirebaseSignupTransactionService.class);
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void savesWholeAggregateAndConsumesEnrollmentInOneNamedTransaction() {
		Aggregate aggregate = aggregate();

		IssuedRefreshSession result = register(aggregate);

		assertThat(result.tokenValue()).isEqualTo("refresh-secret");
		assertThat(transactionManager.commits).isEqualTo(1);
		assertThat(transactionManager.rollbacks).isZero();
		InOrder order = inOrder(
				userRepository,
				firebaseIdentityRepository,
				phoneIdentityRepository,
				aliasRepository,
				outboxRepository,
				socialIdentityRepository,
				refreshSessionIssuer,
				enrollmentRepository
		);
		order.verify(userRepository).save(aggregate.user());
		order.verify(firebaseIdentityRepository).save(aggregate.firebaseIdentity());
		order.verify(phoneIdentityRepository).save(any(PhoneIdentity.class));
		order.verify(aliasRepository).saveAll(any());
		order.verify(outboxRepository).save(aggregate.outbox());
		order.verify(socialIdentityRepository).saveAll(aggregate.socialIdentities());
		order.verify(refreshSessionIssuer).savePrepared(aggregate.preparedSession());
		order.verify(enrollmentRepository).consumeIfPendingAndNotExpired(
				aggregate.attempt().getEnrollmentId(),
				aggregate.attempt().getFirebaseProjectId(),
				aggregate.attempt().getFirebaseUid(),
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				NOW
		);
	}

	@Test
	void outboxFailureRollsBackAndDoesNotStoreSessionOrConsumeEnrollment() {
		doThrow(new DataAccessResourceFailureException("test-only outbox failure"))
				.when(outboxRepository).save(any());

		Throwable failure = catchThrowable(() -> register(aggregate()));

		assertThat(failure).isInstanceOf(DataAccessResourceFailureException.class);
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		assertThat(transactionManager.commits).isZero();
		verify(refreshSessionIssuer, never()).savePrepared(any());
		verify(enrollmentRepository, never()).consumeIfPendingAndNotExpired(
				any(), any(), any(), any(), any(), any()
		);
	}

	@Test
	void failedEnrollmentConsumeRollsBackEvenAfterSessionSave() {
		when(enrollmentRepository.consumeIfPendingAndNotExpired(
				any(), any(), any(), any(), any(), any()
		)).thenReturn(false);

		Throwable failure = catchThrowable(() -> register(aggregate()));

		assertThat(failure)
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT));
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		assertThat(transactionManager.commits).isZero();
		verify(refreshSessionIssuer).savePrepared(any());
	}

	@Test
	void phoneOwnerConflictFailsBeforeAnyAggregatePersistence() {
		when(aliasRepository.findAllActiveByFingerprints(any()))
				.thenReturn(List.of(mock(web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias.class)));

		Throwable failure = catchThrowable(() -> register(aggregate()));

		assertThat(failure)
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.PHONE_ALREADY_LINKED));
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		verify(userRepository, never()).save(any());
		verify(refreshSessionIssuer, never()).savePrepared(any());
	}

	private IssuedRefreshSession register(Aggregate aggregate) {
		return service.register(
				aggregate.user(),
				aggregate.firebaseIdentity(),
				aggregate.phoneFingerprints(),
				aggregate.outbox(),
				aggregate.socialIdentities(),
				aggregate.preparedSession(),
				aggregate.attempt(),
				NOW
		);
	}

	private Aggregate aggregate() {
		User user = User.createFederatedMember(
				"테스트회원",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
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
		PhoneEligibilityBindingOutbox outbox = PhoneEligibilityBindingOutbox.create(
				user.getUserId(),
				"opaque-scope-v1",
				List.of(new PhoneEligibilityFingerprintCandidate("v1", "B".repeat(43))),
				NOW
		);
		List<SocialIdentity> socialIdentities = List.of(SocialIdentity.create(
				user.getUserId(),
				SocialProvider.GOOGLE,
				"google-subject-sensitive",
				NOW
		));
		PreparedRefreshSession prepared = new PreparedRefreshSession(
				"refresh-secret",
				RefreshSession.create(
						user.getUserId(),
						"refresh-hash",
						NOW,
						NOW.plus(Duration.ofDays(14))
				)
		);
		FirebaseEnrollmentAttempt attempt = FirebaseEnrollmentAttempt.create(
				"test-project",
				"firebase-uid-sensitive",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
		return new Aggregate(
				user,
				firebaseIdentity,
				fingerprints,
				outbox,
				socialIdentities,
				prepared,
				attempt
		);
	}

	private record Aggregate(
			User user,
			FirebaseIdentity firebaseIdentity,
			PhoneFingerprintSet phoneFingerprints,
			PhoneEligibilityBindingOutbox outbox,
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

		@Override
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			commits++;
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
			rollbacks++;
		}
	}
}
