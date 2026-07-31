package web.tosunsaeng.identity.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.user.domain.entity.AudioConsent;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

class GuestRegistrationTransactionServiceTests {

	private static final Instant NOW = Instant.parse("2026-07-31T01:00:00Z");
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);

	private UserRepository userRepository;
	private RefreshSessionIssuer refreshSessionIssuer;
	private AuthResponseConverter authResponseConverter;
	private RecordingTransactionManager transactionManager;
	private List<User> committedUsers;
	private List<RefreshSession> committedSessions;
	private AnnotationConfigApplicationContext context;
	private GuestRegistrationTransactionService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		refreshSessionIssuer = mock(RefreshSessionIssuer.class);
		authResponseConverter = mock(AuthResponseConverter.class);
		transactionManager = new RecordingTransactionManager();
		committedUsers = new CopyOnWriteArrayList<>();
		committedSessions = new CopyOnWriteArrayList<>();

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			committedUsers.add(user);
			removeAfterRollback(committedUsers, user);
			return user;
		});
		when(refreshSessionIssuer.savePrepared(any())).thenAnswer(invocation -> {
			PreparedRefreshSession prepared = invocation.getArgument(0);
			RefreshSession session = prepared.session();
			committedSessions.add(session);
			removeAfterRollback(committedSessions, session);
			return new IssuedRefreshSession(
					prepared.tokenValue(),
					session.getCreatedAt(),
					session.getExpiresAt()
			);
		});
		when(authResponseConverter.toGuestAuthResponse(any(), any(), any(), any()))
				.thenReturn(response());

		context = new AnnotationConfigApplicationContext();
		context.register(TransactionTestConfiguration.class);
		context.registerBean(UserRepository.class, () -> userRepository);
		context.registerBean(RefreshSessionIssuer.class, () -> refreshSessionIssuer);
		context.registerBean(AuthResponseConverter.class, () -> authResponseConverter);
		context.registerBean(
				"mongoTransactionManager",
				PlatformTransactionManager.class,
				() -> transactionManager
		);
		context.registerBean(GuestRegistrationTransactionService.class);
		context.refresh();
		service = context.getBean(GuestRegistrationTransactionService.class);
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void commitsUserSessionAndPreparedResponseTogether() {
		User guest = guest();
		PreparedRefreshSession prepared = prepared(guest.getUserId());

		GuestAuthResponse result = service.register(guest, accessToken(), prepared);

		assertThat(result).isEqualTo(response());
		assertThat(committedUsers).containsExactly(guest);
		assertThat(committedSessions).containsExactly(prepared.session());
		assertThat(transactionManager.commits).isEqualTo(1);
		assertThat(transactionManager.rollbacks).isZero();
		InOrder order = inOrder(userRepository, refreshSessionIssuer, authResponseConverter);
		order.verify(userRepository).save(guest);
		order.verify(refreshSessionIssuer).savePrepared(prepared);
		order.verify(authResponseConverter).toGuestAuthResponse(any(), any(), any(), any());
	}

	@Test
	void userSaveFailureRollsBackAndNeverStoresRefreshSession() {
		doThrow(new DataAccessResourceFailureException("test-only user save failure"))
				.when(userRepository).save(any(User.class));
		User guest = guest();

		Throwable failure = catchThrowable(
				() -> service.register(guest, accessToken(), prepared(guest.getUserId()))
		);

		assertThat(failure).isInstanceOf(DataAccessResourceFailureException.class);
		assertThat(committedUsers).isEmpty();
		assertThat(committedSessions).isEmpty();
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		verify(refreshSessionIssuer, never()).savePrepared(any());
	}

	@Test
	void refreshSessionSaveFailureRollsBackUserAndAllowsSuccessfulRetry() {
		doThrow(new DataAccessResourceFailureException("test-only session save failure"))
				.doAnswer(invocation -> {
					PreparedRefreshSession prepared = invocation.getArgument(0);
					committedSessions.add(prepared.session());
					removeAfterRollback(committedSessions, prepared.session());
					return new IssuedRefreshSession(
							prepared.tokenValue(),
							prepared.session().getCreatedAt(),
							prepared.session().getExpiresAt()
					);
				})
				.when(refreshSessionIssuer).savePrepared(any());
		User firstGuest = guest();

		Throwable failure = catchThrowable(
				() -> service.register(
						firstGuest,
						accessToken(),
						prepared(firstGuest.getUserId())
				)
		);
		assertThat(failure).isInstanceOf(DataAccessResourceFailureException.class);
		assertThat(committedUsers).isEmpty();
		assertThat(committedSessions).isEmpty();
		assertThat(transactionManager.rollbacks).isEqualTo(1);

		User retryGuest = guest();
		GuestAuthResponse retry = service.register(
				retryGuest,
				accessToken(),
				prepared(retryGuest.getUserId())
		);
		assertThat(retry).isEqualTo(response());
		assertThat(committedUsers).containsExactly(retryGuest);
		assertThat(committedSessions).hasSize(1);
		assertThat(transactionManager.commits).isEqualTo(1);
	}

	@Test
	void responseConstructionFailureRollsBackBothDocuments() {
		when(authResponseConverter.toGuestAuthResponse(any(), any(), any(), any())).thenThrow(
				new IllegalStateException("test-only response construction failure")
		);
		User guest = guest();

		Throwable failure = catchThrowable(
				() -> service.register(guest, accessToken(), prepared(guest.getUserId()))
		);

		assertThat(failure).isInstanceOf(IllegalStateException.class);
		assertThat(committedUsers).isEmpty();
		assertThat(committedSessions).isEmpty();
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		assertThat(transactionManager.commits).isZero();
	}

	@Test
	void mismatchedPreparedSessionFailsBeforeAnyPersistence() {
		User guest = guest();

		Throwable failure = catchThrowable(
				() -> service.register(guest, accessToken(), prepared(otherGuest().getUserId()))
		);

		assertThat(failure).isInstanceOf(IllegalArgumentException.class);
		assertThat(committedUsers).isEmpty();
		assertThat(committedSessions).isEmpty();
		assertThat(transactionManager.rollbacks).isEqualTo(1);
		verify(userRepository, never()).save(any());
	}

	@Test
	void missingNamedTransactionManagerFailsBeforePersistenceInsteadOfRunningNonAtomically() {
		try (AnnotationConfigApplicationContext noManagerContext =
					new AnnotationConfigApplicationContext()) {
			noManagerContext.register(TransactionTestConfiguration.class);
			noManagerContext.registerBean(UserRepository.class, () -> userRepository);
			noManagerContext.registerBean(RefreshSessionIssuer.class, () -> refreshSessionIssuer);
			noManagerContext.registerBean(AuthResponseConverter.class, () -> authResponseConverter);
			noManagerContext.registerBean(GuestRegistrationTransactionService.class);
			noManagerContext.refresh();
			GuestRegistrationTransactionService noManagerService = noManagerContext.getBean(
					GuestRegistrationTransactionService.class
			);
			User guest = guest();

			org.assertj.core.api.Assertions.assertThatThrownBy(() -> noManagerService.register(
					guest,
					accessToken(),
					prepared(guest.getUserId())
			))
					.isInstanceOf(NoSuchBeanDefinitionException.class);
			verify(userRepository, never()).save(any());
			verify(refreshSessionIssuer, never()).savePrepared(any());
		}
	}

	private <T> void removeAfterRollback(List<T> values, T value) {
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
					values.remove(value);
				}
			}
		});
	}

	private User guest() {
		return User.createGuest(
				"A".repeat(43),
				"게스트",
				AudioConsent.agreed("test-audio-policy-v1", NOW),
				NOW
		);
	}

	private User otherGuest() {
		return User.createGuest(
				"B".repeat(43),
				"게스트",
				AudioConsent.agreed("test-audio-policy-v1", NOW),
				NOW
		);
	}

	private PreparedRefreshSession prepared(String userId) {
		return new PreparedRefreshSession(
				"prepared-refresh-test-value",
				RefreshSession.create(
						userId,
						"prepared-refresh-test-hash",
						NOW,
						NOW.plus(REFRESH_TTL)
				)
		);
	}

	private IssuedAccessToken accessToken() {
		return new IssuedAccessToken(
				"prepared-access-test-value",
				"Bearer",
				NOW,
				NOW.plus(ACCESS_TTL),
				ACCESS_TTL.toSeconds()
		);
	}

	private GuestAuthResponse response() {
		return new GuestAuthResponse(
				"prepared-access-test-value",
				"prepared-refresh-test-value",
				"Bearer",
				ACCESS_TTL.toMillis(),
				REFRESH_TTL.toMillis()
		);
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
