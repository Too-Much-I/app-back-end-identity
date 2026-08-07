package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;

class UserWithdrawalTransactionServiceTests {

	private static final Instant CREATED_AT = Instant.parse("2026-08-01T00:00:00Z");
	private static final Instant WITHDRAWN_AT = Instant.parse("2026-08-07T01:23:45Z");
	private static final String CREDENTIAL_HASH = "withdrawal-credential-token-hash";

	private UserRepository userRepository;
	private RefreshSessionRepository sessionRepository;
	private RecordingTransactionManager transactionManager;
	private AtomicReference<User> persistedTombstone;
	private AtomicReference<List<RefreshSession>> persistedSessions;
	private AnnotationConfigApplicationContext context;
	private UserWithdrawalTransactionService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		sessionRepository = mock(RefreshSessionRepository.class);
		transactionManager = new RecordingTransactionManager();
		persistedTombstone = new AtomicReference<>();
		persistedSessions = new AtomicReference<>(List.of());

		when(userRepository.withdrawIfUnchanged(any(), any(), any())).thenAnswer(invocation -> {
			User tombstone = invocation.getArgument(0);
			persistedTombstone.set(tombstone);
			clearAfterRollback(persistedTombstone);
			return true;
		});
		when(sessionRepository.saveAll(any())).thenAnswer(invocation -> {
			List<RefreshSession> sessions = invocation.getArgument(0);
			persistedSessions.set(List.copyOf(sessions));
			clearAfterRollback(persistedSessions);
			return sessions;
		});

		context = new AnnotationConfigApplicationContext();
		context.register(TransactionTestConfiguration.class);
		context.registerBean(UserRepository.class, () -> userRepository);
		context.registerBean(RefreshSessionRepository.class, () -> sessionRepository);
		context.registerBean(
				"mongoTransactionManager",
				PlatformTransactionManager.class,
				() -> transactionManager
		);
		context.registerBean(UserWithdrawalTransactionService.class);
		context.refresh();
		service = context.getBean(UserWithdrawalTransactionService.class);
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void atomicallyStoresTombstoneAndRevokesEveryOwnedActiveSession() {
		User user = localUser();
		RefreshSession credential = activeSession(user.getUserId(), CREDENTIAL_HASH);
		RefreshSession second = activeSession(user.getUserId(), "second-session-hash");
		RefreshSession third = activeSession(user.getUserId(), "third-session-hash");
		RefreshSession other = activeSession(guestUser().getUserId(), "other-session-hash");
		when(sessionRepository.findByTokenHash(CREDENTIAL_HASH))
				.thenReturn(Optional.of(credential));
		when(sessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getUserId()))
				.thenReturn(List.of(credential, second, third));

		WithdrawalTransactionResult result = service.withdraw(
				user,
				CREDENTIAL_HASH,
				WITHDRAWN_AT
		);
		WithdrawResponse response = result.response();

		assertThat(AopUtils.isAopProxy(service)).isTrue();
		assertThat(transactionManager.commits).isEqualTo(1);
		assertThat(transactionManager.rollbacks).isZero();
		assertThat(response.status()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(response.withdrawnAt()).isEqualTo(WITHDRAWN_AT);
		assertThat(result.revokedSessionCount()).isEqualTo(3);
		assertThat(persistedTombstone.get().getEmail()).isNull();
		assertThat(persistedTombstone.get().getNormalizedEmail()).isNull();
		assertThat(persistedTombstone.get().getPasswordHash()).isNull();
		assertThat(persistedTombstone.get().getGuestInstallationIdHash()).isNull();
		assertThat(persistedTombstone.get().getConsents()).isSameAs(user.getConsents());
		assertThat(persistedSessions.get()).containsExactly(credential, second, third);
		assertThat(persistedSessions.get()).allSatisfy(session -> {
			assertThat(session.getRevocationReason())
					.isEqualTo(RevocationReason.ACCOUNT_WITHDRAWN);
			assertThat(session.getRevokedAt()).isEqualTo(WITHDRAWN_AT);
			assertThat(session.getLastUsedAt()).isEqualTo(WITHDRAWN_AT);
		});
		assertThat(other.isRevoked()).isFalse();
		InOrder order = inOrder(userRepository, sessionRepository);
		order.verify(userRepository).withdrawIfUnchanged(
				any(User.class),
				eq(UserStatus.ACTIVE),
				eq(CREATED_AT)
		);
		order.verify(sessionRepository).findAllByUserIdAndRevokedAtIsNull(user.getUserId());
		order.verify(sessionRepository).saveAll(List.of(credential, second, third));
	}

	@Test
	void userConditionalUpdateFailureRollsBackBeforeSessionMutation() {
		User user = localUser();
		RefreshSession credential = activeSession(user.getUserId(), CREDENTIAL_HASH);
		when(sessionRepository.findByTokenHash(CREDENTIAL_HASH))
				.thenReturn(Optional.of(credential));
		doReturn(false).when(userRepository).withdrawIfUnchanged(any(), any(), any());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(user, CREDENTIAL_HASH, WITHDRAWN_AT)
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.WITHDRAWAL_CONFLICT);
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(credential.isRevoked()).isFalse();
		verify(sessionRepository, never()).findAllByUserIdAndRevokedAtIsNull(any());
		verify(sessionRepository, never()).saveAll(any());
	}

	@Test
	void sessionSaveFailureRollsBackPersistedTombstone() {
		User user = localUser();
		RefreshSession credential = activeSession(user.getUserId(), CREDENTIAL_HASH);
		when(sessionRepository.findByTokenHash(CREDENTIAL_HASH))
				.thenReturn(Optional.of(credential));
		when(sessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getUserId()))
				.thenReturn(List.of(credential));
		doThrow(new DataAccessResourceFailureException("test-only session save failure"))
				.when(sessionRepository).saveAll(any());

		DataAccessResourceFailureException exception = catchThrowableOfType(
				DataAccessResourceFailureException.class,
				() -> service.withdraw(user, CREDENTIAL_HASH, WITHDRAWN_AT)
		);

		assertThat(exception).isNotNull();
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		assertThat(transactionManager.commits).isZero();
		assertThat(persistedTombstone.get()).isNull();
		assertThat(persistedSessions.get()).isEmpty();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void userPersistenceFailureDoesNotTouchSessions() {
		User user = localUser();
		RefreshSession credential = activeSession(user.getUserId(), CREDENTIAL_HASH);
		when(sessionRepository.findByTokenHash(CREDENTIAL_HASH))
				.thenReturn(Optional.of(credential));
		doThrow(new DataAccessResourceFailureException("test-only user update failure"))
				.when(userRepository).withdrawIfUnchanged(any(), any(), any());

		catchThrowableOfType(
				DataAccessResourceFailureException.class,
				() -> service.withdraw(user, CREDENTIAL_HASH, WITHDRAWN_AT)
		);

		assertThat(transactionManager.rollbacks).isEqualTo(1);
		assertThat(persistedTombstone.get()).isNull();
		verify(sessionRepository, never()).findAllByUserIdAndRevokedAtIsNull(any());
		verify(sessionRepository, never()).saveAll(any());
	}

	@Test
	void concurrentSessionChangeReturnsWithdrawalConflictAndRollsBackUser() {
		User user = localUser();
		RefreshSession credential = activeSession(user.getUserId(), CREDENTIAL_HASH);
		when(sessionRepository.findByTokenHash(CREDENTIAL_HASH))
				.thenReturn(Optional.of(credential));
		when(sessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getUserId()))
				.thenReturn(List.of(credential));
		doThrow(new OptimisticLockingFailureException("test-only concurrent session change"))
				.when(sessionRepository).saveAll(any());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(user, CREDENTIAL_HASH, WITHDRAWN_AT)
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.WITHDRAWAL_CONFLICT);
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		assertThat(persistedTombstone.get()).isNull();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void credentialIsRecheckedInsideTransaction() {
		User user = localUser();
		RefreshSession revoked = activeSession(user.getUserId(), CREDENTIAL_HASH);
		revoked.logout(WITHDRAWN_AT.minusSeconds(1));
		when(sessionRepository.findByTokenHash(CREDENTIAL_HASH))
				.thenReturn(Optional.of(revoked));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(user, CREDENTIAL_HASH, WITHDRAWN_AT)
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		verify(userRepository, never()).withdrawIfUnchanged(any(), any(), any());
	}

	private <T> void clearAfterRollback(AtomicReference<T> value) {
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCompletion(int status) {
						if (status == STATUS_ROLLED_BACK) {
							value.set(null);
						}
					}
				}
		);
	}

	private User localUser() {
		return User.create(
				"transaction.user@example.test",
				"transaction.user@example.test",
				"encoded-password",
				"트랜잭션테스트",
				UserConsents.consented("privacy-v1", "term-v1", CREATED_AT),
				CREATED_AT
		);
	}

	private User guestUser() {
		return User.createGuest(
				"A".repeat(43),
				"게스트",
				UserConsents.consented("privacy-v1", "term-v1", CREATED_AT),
				CREATED_AT
		);
	}

	private RefreshSession activeSession(String userId, String tokenHash) {
		return RefreshSession.create(
				userId,
				tokenHash,
				CREATED_AT,
				WITHDRAWN_AT.plusSeconds(3600)
		);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class TransactionTestConfiguration {
	}

	static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

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
