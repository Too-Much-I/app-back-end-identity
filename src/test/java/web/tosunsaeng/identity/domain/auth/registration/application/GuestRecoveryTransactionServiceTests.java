package web.tosunsaeng.identity.domain.auth.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Set;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.TokenReissueService;
import web.tosunsaeng.identity.domain.auth.session.application.SessionAuthentication;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

class GuestRecoveryTransactionServiceTests {
	private final MongoTemplate mongo = mock(MongoTemplate.class);
	private final AccessTokenIssuer access = mock(AccessTokenIssuer.class);
	private final RefreshSessionIssuer refresh = mock(RefreshSessionIssuer.class);
	private final Instant now = Instant.parse("2026-09-18T00:00:00Z");
	private final String hash = "A".repeat(43);
	private final User guest = User.createGuest(hash, "guest",
			UserConsents.consented("privacy-v1", "term-v1", now), now);

	private GuestRecoveryTransactionService service(boolean enabled) {
		return new GuestRecoveryTransactionService(mongo, access, refresh, new AuthResponseConverter(), enabled);
	}

	private void success() {
		when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
				.thenReturn(guest);
		when(access.issue(guest.getUserId(), UserAccountType.GUEST, Set.of())).thenReturn(
				new IssuedAccessToken("test-access", "Bearer", now, now.plusSeconds(1800), 1800));
		when(refresh.issue(guest.getUserId())).thenReturn(
				new IssuedRefreshSession("test-refresh", now, now.plusSeconds(1209600)));
	}

	@Test
	void fencedRecoveryUsesCurrentEpochAndGuestAuthentication() {
		success();
		when(refresh.isFenceEnabled()).thenReturn(true);
		when(refresh.captureEpoch(guest.getUserId())).thenReturn(7L);
		when(refresh.issueAuthenticated(eq(guest.getUserId()), any())).thenReturn(
				new IssuedRefreshSession("test-current-epoch", now, now.plusSeconds(1209600)));
		assertThat(service(true).recover(hash).refreshToken()).isEqualTo("test-current-epoch");
		verify(refresh).issueAuthenticated(guest.getUserId(), new SessionAuthentication(
				7L, SessionAuthentication.Source.GUEST, null, null));
		verify(refresh, never()).issue(any());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = UserStatus.class, names = {"SUSPENDED", "WITHDRAWN", "MERGED"})
	void refusesNonActiveUsers(UserStatus status) {
		success();
		org.springframework.test.util.ReflectionTestUtils.setField(guest, "status", status);
		assertThatThrownBy(() -> service(true).recover(hash)).isInstanceOf(AuthException.class);
		verifyNoInteractions(access, refresh);
	}

	@Test
	void refusesPromotedMemberEvenWithStaleInstallationHash() {
		success();
		org.springframework.test.util.ReflectionTestUtils.setField(guest, "accountType", UserAccountType.MEMBER);
		assertThatThrownBy(() -> service(true).recover(hash)).isInstanceOf(AuthException.class);
		verifyNoInteractions(access, refresh);
	}

	@Test
	void refusesMergeMetadataEvenIfStatusIsStillActive() {
		success();
		org.springframework.test.util.ReflectionTestUtils.setField(guest, "mergedIntoUserId", java.util.UUID.randomUUID().toString());
		assertThatThrownBy(() -> service(true).recover(hash)).isInstanceOf(AuthException.class);
		verifyNoInteractions(access, refresh);
	}

	@Test
	void legacyGuestWithoutAccountTypeStillRecovers() {
		success();
		org.springframework.test.util.ReflectionTestUtils.setField(guest, "accountType", null);
		assertThat(service(true).recover(hash).accessToken()).isEqualTo("test-access");
	}

	@Test
	void disabledDoesNotReadOrWriteOrIssue() {
		assertThatThrownBy(() -> service(false).recover(hash)).isInstanceOf(AuthException.class);
		verifyNoInteractions(mongo, access, refresh);
	}

	@Test
	void activeGuestGuardIsAtomicAndPreservesIdentityAndConsents() {
		success();
		var response = service(true).recover(hash);
		assertThat(response.accessToken()).isEqualTo("test-access");
		assertThat(response.refreshTokenExpiresIn()).isEqualTo(1209600000L);
		var query = ArgumentCaptor.forClass(Query.class);
		var update = ArgumentCaptor.forClass(Update.class);
		var options = ArgumentCaptor.forClass(FindAndModifyOptions.class);
		verify(mongo).findAndModify(query.capture(), update.capture(), options.capture(), eq(User.class));
		assertThat(query.getValue().getQueryObject()).containsEntry("guestInstallationIdHash", hash)
				.containsEntry("provider", UserProvider.GUEST).containsEntry("status", UserStatus.ACTIVE)
				.containsEntry("mergedIntoUserId", null).containsEntry("mergedAt", null);
		assertThat(query.getValue().getQueryObject().get("$or")).isEqualTo(java.util.List.of(
				new org.bson.Document("accountType", UserAccountType.GUEST), new org.bson.Document("accountType", null)));
		assertThat(update.getValue().getUpdateObject().toJson()).isEqualTo("{\"$inc\": {\"guestRecoveryFence\": 1}}");
		assertThat(options.getValue().isUpsert()).isFalse();
		verify(access).issue(guest.getUserId(), UserAccountType.GUEST, Set.of());
		verify(refresh).issue(guest.getUserId());
		assertThat(guest.getConsents().getPrivacyConsentVersion()).isEqualTo("privacy-v1");
	}

	@Test
	void noEligibleGuestIncludingChangedOrWithdrawnOwnerIsRejected() {
		assertThatThrownBy(() -> service(true).recover(hash)).isInstanceOf(AuthException.class);
		verifyNoInteractions(access, refresh);
	}

	@Test
	void concurrentStatusWriteConflictDoesNotIssueTokens() {
		when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(User.class)))
				.thenThrow(new OptimisticLockingFailureException("test write conflict"));
		assertThatThrownBy(() -> service(true).recover(hash)).isInstanceOf(OptimisticLockingFailureException.class);
		verifyNoInteractions(access, refresh);
	}

	@Test
	void defaultFlagIsOffWhenSpringPropertyIsAbsent() {
		try (var context = new AnnotationConfigApplicationContext()) {
			context.registerBean(MongoTemplate.class, () -> mongo);
			context.registerBean(AccessTokenIssuer.class, () -> access);
			context.registerBean(RefreshSessionIssuer.class, () -> refresh);
			context.registerBean(AuthResponseConverter.class);
			context.registerBean(GuestRecoveryTransactionService.class);
			context.refresh();
			clearInvocations(mongo);
			assertThatThrownBy(() -> context.getBean(GuestRecoveryTransactionService.class).recover(hash))
					.isInstanceOf(AuthException.class);
			verifyNoInteractions(mongo, access, refresh);
		}
	}

	@Test
	void recoveredSessionStillRotatesAfterRecoveryIsDisabledWithoutRevokingExistingSessions() {
		var sessions = mock(web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository.class);
		var users = mock(web.tosunsaeng.identity.domain.user.domain.repository.UserRepository.class);
		var generator = mock(web.tosunsaeng.identity.global.security.refresh.RefreshTokenGenerator.class);
		var hasher = new web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher();
		var clock = java.time.Clock.fixed(now, java.time.ZoneOffset.UTC);
		var issuer = new RefreshSessionIssuer(generator, hasher, sessions,
				new web.tosunsaeng.identity.global.security.refresh.RefreshTokenProperties(java.time.Duration.ofDays(14), 32), clock);
		var stored = new java.util.concurrent.ConcurrentHashMap<String, web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession>();
		when(sessions.save(any())).thenAnswer(invocation -> {
			web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession value = invocation.getArgument(0);
			stored.put(value.getTokenHash(), value);
			return value;
		});
		when(sessions.findByTokenHash(any())).thenAnswer(invocation -> java.util.Optional.ofNullable(stored.get(invocation.getArgument(0))));
		when(users.findById(guest.getUserId())).thenReturn(java.util.Optional.of(guest));
		when(generator.generate()).thenReturn("test-recovered-refresh", "test-rotated-refresh");
		success();
		var converter = new AuthResponseConverter();
		var recovered = new GuestRecoveryTransactionService(mongo, access, issuer, converter, true).recover(hash);
		var original = stored.get(hasher.hash(recovered.refreshToken()));
		assertThat(original.isRevoked()).isFalse();
		assertThat(original.getTokenHash()).isNotEqualTo(recovered.refreshToken());
		assertThatThrownBy(() -> new GuestRecoveryTransactionService(mongo, access, issuer, converter, false).recover(hash))
				.isInstanceOf(AuthException.class);
		assertThat(original.isRevoked()).isFalse();
		var rotated = new TokenReissueService(hasher, sessions, users, access, issuer, converter, clock)
				.reissue(new web.tosunsaeng.identity.domain.auth.session.dto.request.ReissueRequest(recovered.refreshToken()));
		assertThat(rotated.refreshToken()).isEqualTo("test-rotated-refresh");
		assertThat(stored.get(hasher.hash(rotated.refreshToken())).getUserId()).isEqualTo(guest.getUserId());
		verify(sessions, never()).saveAll(any());
	}

	@Test
	void parallelRecoveriesKeepSameUserAndIssueSeparateSessions() throws Exception {
		success();
		var start = new java.util.concurrent.CountDownLatch(1);
		var counter = new java.util.concurrent.atomic.AtomicInteger();
		when(refresh.issue(guest.getUserId())).thenAnswer(invocation ->
				new IssuedRefreshSession("test-refresh-" + counter.incrementAndGet(), now, now.plusSeconds(1209600)));
		try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
			var recovery = service(true);
			var first = pool.submit(() -> { start.await(); return recovery.recover(hash); });
			var second = pool.submit(() -> { start.await(); return recovery.recover(hash); });
			start.countDown();
			assertThat(first.get(5, java.util.concurrent.TimeUnit.SECONDS).refreshToken())
					.isNotEqualTo(second.get(5, java.util.concurrent.TimeUnit.SECONDS).refreshToken());
		}
		verify(refresh, times(2)).issue(guest.getUserId());
		verify(access, times(2)).issue(guest.getUserId(), UserAccountType.GUEST, Set.of());
	}

	@Test
	void transactionCommitsOnSuccessAndRollsBackOnSessionFailure() {
		success();
		var manager = new RecordingManager();
		try (var context = new AnnotationConfigApplicationContext()) {
			context.register(TxConfig.class);
			context.registerBean("mongoTransactionManager", RecordingManager.class, () -> manager);
			context.registerBean(GuestRecoveryTransactionService.class, () -> service(true));
			context.refresh();
			var proxy = context.getBean(GuestRecoveryTransactionService.class);
			when(refresh.issue(guest.getUserId())).thenAnswer(invocation -> {
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				return new IssuedRefreshSession("test-refresh", now, now.plusSeconds(1209600));
			});
			proxy.recover(hash);
			assertThat(manager.commits).isEqualTo(1);
			doThrow(new DataAccessResourceFailureException("test failure"))
					.when(refresh).issue(guest.getUserId());
			assertThatThrownBy(() -> proxy.recover(hash)).isInstanceOf(DataAccessResourceFailureException.class);
			assertThat(manager.rollbacks).isEqualTo(1);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class TxConfig {}

	static class RecordingManager extends AbstractPlatformTransactionManager {
		int commits;
		int rollbacks;
		@Override protected Object doGetTransaction() { return new Object(); }
		@Override protected void doBegin(Object tx, TransactionDefinition definition) {}
		@Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
		@Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
	}
}
