package web.tosunsaeng.identity.domain.auth.session.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.domain.*;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.SessionRevocationProperties;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.refresh.*;

/** In-memory Mongo verifies mapping/index/CAS. It does not prove replica-set transaction rollback. */
class SessionRevocationTests {
	static final String USER = "00000000-0000-4000-8000-000000000001";
	static final Instant NOW = Instant.parse("2026-09-08T00:00:00Z");
	MongoServer server; MongoClient client; MongoTemplate mongo;
	SessionSecurityService security; LogoutAllCoordinator coordinator; FirebaseSessionRevocationWorker worker;
	FirebaseIdentityRepository identities; UserRepository users; FirebaseIdentity binding;
	FirebaseSessionRevocationPort port; RefreshSessionIssuer issuer;
	AtomicReference<Instant> time; Clock clock; TransactionTemplate tx;
	SessionRevocationProperties properties;

	@BeforeEach void setup() {
		server = new MongoServer(new MemoryBackend()); var address = server.bind();
		client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
		mongo = new MongoTemplate(client, "session-revocation-test");
		for (Class<?> type : List.of(UserSessionControl.class, LogoutAllOperation.class, RefreshSession.class)) {
			new MongoPersistentEntityIndexResolver(mongo.getConverter().getMappingContext()).resolveIndexFor(type)
					.forEach(index -> {
						// mongo-java-server 1.47 does not enforce partialFilterExpression. Installing
						// this as an unconditional unique index would reject unrelated legacy sessions.
						if (!"uk_refresh_user_rotation_request".equals(index.getIndexOptions().getString("name"))) {
							mongo.indexOps(type).ensureIndex(index);
						}
					});
		}
		time = new AtomicReference<>(NOW);
		clock = new Clock() {
			public ZoneId getZone() { return ZoneOffset.UTC; }
			public Clock withZone(ZoneId zone) { return this; }
			public Instant instant() { return time.get(); }
		};
		tx = mock(TransactionTemplate.class);
		when(tx.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(new SimpleTransactionStatus()));
		users = mock(UserRepository.class); User user = mock(User.class);
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(users.findById(USER)).thenReturn(Optional.of(user));
		identities = mock(FirebaseIdentityRepository.class);
		binding = FirebaseIdentity.create("test-project", "test-uid", USER, NOW.minusSeconds(3600));
		when(identities.findByUserId(USER)).thenReturn(Optional.of(binding));
		when(identities.findById(binding.getFirebaseIdentityId())).thenReturn(Optional.of(binding));
		security = new SessionSecurityService(mongo, tx, users, identities);
		properties = new SessionRevocationProperties();
		coordinator = new LogoutAllCoordinator(security, mongo, identities, properties, null, clock);
		port = mock(FirebaseSessionRevocationPort.class);
		when(port.inspect(any())).thenAnswer(invocation -> new FirebaseSessionRevocationPort.Snapshot(
				NOW.minusSeconds(3600), time.get(), false));
		worker = new FirebaseSessionRevocationWorker(security, mongo, identities, port, properties, coordinator, clock);
		RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
		when(sessions.save(any())).thenAnswer(invocation -> mongo.save(invocation.getArgument(0, RefreshSession.class)));
		RefreshTokenGenerator generator = mock(RefreshTokenGenerator.class);
		when(generator.generate()).thenAnswer(invocation -> UUID.randomUUID().toString());
		issuer = new RefreshSessionIssuer(generator, new RefreshTokenHasher(), sessions,
				new RefreshTokenProperties(Duration.ofDays(14), 32), clock);
		issuer.setSessionSecurity(security);
	}
	@AfterEach void close() { client.close(); server.shutdownNow(); }

	@Test void requestWithNoLocalSessionsStillCreatesFirebaseJob() {
		accept("first"); assertThat(operations()).hasSize(1);
		assertThat(control().getSessionEpoch()).isEqualTo(1);
		assertThat(control().getMinimumFirebaseAuthTimeExclusive()).isEqualTo(NOW);
		assertThat(operations().get(0).getCleanupAt()).isNull();
	}
	@Test void sameRequestIsNoopEvenAfterRemoteCompletion() {
		accept("first"); String id = operations().get(0).getLogoutId();
		worker.process(id); accept("first"); worker.process(id);
		assertThat(operations()).hasSize(1); assertThat(control().getSessionEpoch()).isEqualTo(1);
		verify(port, times(1)).revoke(any(), any());
	}
	@Test void differentRequestDuringPendingCreatesNewEpochAndQueuedOperation() {
		accept("first"); var first = operations().get(0);
		accept("second"); assertThat(operations()).hasSize(2); assertThat(control().getSessionEpoch()).isEqualTo(2);
		var second = operations().stream().filter(op -> op.getEpoch() == 2).findFirst().orElseThrow();
		worker.process(second.getLogoutId()); verifyNoInteractions(port);
		worker.process(first.getLogoutId()); time.set(NOW.plus(properties.getLease())); worker.process(second.getLogoutId());
		verify(port, times(2)).revoke(any(), any()); assertThat(control().getSessionEpoch()).isEqualTo(2);
	}
	@Test void blockedUserQueueDoesNotStarveAnotherUser() {
		accept("first");
		var blocked = operations().get(0); blocked.unknown("UNRESOLVED_DISPATCH"); mongo.save(blocked);
		for (int i = 0; i < properties.getBatchSize(); i++) accept("queued-" + i);
		String other = "00000000-0000-4000-8000-000000000002";
		User user = mock(User.class); when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(users.findById(other)).thenReturn(Optional.of(user));
		var otherBinding = FirebaseIdentity.create("test-project", "other-uid", other, NOW.minusSeconds(3600));
		when(identities.findByUserId(other)).thenReturn(Optional.of(otherBinding));
		when(identities.findById(otherBinding.getFirebaseIdentityId())).thenReturn(Optional.of(otherBinding));
		time.set(NOW.plusSeconds(1)); coordinator.accept(other, "other-request", NOW.plusSeconds(1800));
		worker.runBatch();
		time.set(NOW.plusSeconds(6)); worker.runBatch();
		assertThat(operations().stream().filter(op -> op.getUserId().equals(other)).findFirst().orElseThrow().getStatus())
				.isEqualTo(LogoutAllOperation.Status.COMPLETED);
		verify(port, times(1)).revoke(argThat(target -> target.uid().equals("other-uid")), any());
	}
	@Test void freshLoginWhilePendingSucceedsButOldAuthenticationAndOldEpochFail() {
		accept("first");
		assertThatCode(() -> security.checkAndTouch(firebaseSession(1, NOW.plusSeconds(2)), true)).doesNotThrowAnyException();
		assertError(() -> security.checkAndTouch(firebaseSession(1, NOW), true), AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED);
		assertError(() -> security.checkAndTouch(firebaseSession(0, NOW.plusSeconds(2)), true), AuthErrorStatus.SESSION_LOGGED_OUT);
	}
	@Test void delayedRevokeOnlyInvalidatesAffectedFirebaseSessions() {
		accept("first"); time.set(NOW.plusSeconds(10)); worker.process(operations().get(0).getLogoutId());
		assertError(() -> security.checkAndTouch(firebaseSession(1, NOW.plusSeconds(5)), false), AuthErrorStatus.SESSION_LOGGED_OUT);
		assertThatCode(() -> security.checkAndTouch(firebaseSession(1, NOW.plusSeconds(11)), false)).doesNotThrowAnyException();
		assertThatCode(() -> security.checkAndTouch(localSession(1), false)).doesNotThrowAnyException();
		assertThat(control().getSessionEpoch()).isEqualTo(1);
	}
	@Test void lateObservationDoesNotUseCompletionTimeAsAuthenticationCutoff() {
		accept("first"); time.set(NOW.plusSeconds(10));
		doAnswer(invocation -> { time.set(NOW.plusSeconds(100)); return null; }).when(port).revoke(any(), any());
		when(port.inspect(any())).thenReturn(new FirebaseSessionRevocationPort.Snapshot(NOW.minusSeconds(3600), NOW.plusSeconds(10), false));
		worker.process(operations().get(0).getLogoutId());
		assertThat(control().getConfirmedFirebaseRevocationBoundary()).isEqualTo(NOW.plusSeconds(10));
		assertThatCode(() -> security.checkAndTouch(firebaseSession(1, NOW.plusSeconds(11)), true)).doesNotThrowAnyException();
	}
	@Test void unknownMutationIsNotRedispatchedAndDoesNotBlockFreshLogin() {
		accept("first"); var id = operations().get(0).getLogoutId();
		doThrow(new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.RESULT_UNKNOWN)).when(port).revoke(any(), any());
		worker.process(id); time.set(NOW.plusSeconds(120)); worker.process(id);
		assertThat(operation(id).getStatus()).isEqualTo(LogoutAllOperation.Status.RECONCILIATION_REQUIRED);
		assertThat(operation(id).getCleanupAt()).isNull(); verify(port, times(1)).revoke(any(), any());
		assertThatCode(() -> security.checkAndTouch(firebaseSession(1, NOW.plusSeconds(121)), true)).doesNotThrowAnyException();
	}
	@Test void crashedAfterMutationRecordCannotBeRedispatched() {
		accept("first"); var op = operations().get(0); op.claim("stale", NOW.minusSeconds(1));
		op.startMutation(NOW.minusSeconds(3600), NOW); mongo.save(op);
		worker.process(op.getLogoutId()); verifyNoInteractions(port);
		assertThat(operation(op.getLogoutId()).getStatus()).isEqualTo(LogoutAllOperation.Status.RECONCILIATION_REQUIRED);
	}
	@Test void expiredPredispatchClaimCanBeReclaimed() {
		accept("first"); var op = operations().get(0); op.claim("stale", NOW.minusSeconds(1)); mongo.save(op);
		worker.process(op.getLogoutId()); verify(port).revoke(any(), any());
	}
	@Test void completedWorkerDoesNotOverwriteNewLogoutWatermark() {
		accept("first"); var first = operations().get(0); time.set(NOW.plusSeconds(60)); accept("second");
		worker.process(first.getLogoutId());
		assertThat(control().getMinimumFirebaseAuthTimeExclusive()).isEqualTo(NOW.plusSeconds(60));
		assertThat(control().getSessionEpoch()).isEqualTo(2);
	}
	@Test void exactBindingMismatchPreventsExternalCall() {
		accept("first"); when(identities.findById(binding.getFirebaseIdentityId())).thenReturn(Optional.empty());
		worker.process(operations().get(0).getLogoutId()); verifyNoInteractions(port);
		assertThat(operations().get(0).getStatus()).isEqualTo(LogoutAllOperation.Status.RECONCILIATION_REQUIRED);
	}
	@Test void disabledRemoteIsNotTreatedAsSuccessfulLogout() {
		accept("first"); when(port.inspect(any())).thenReturn(new FirebaseSessionRevocationPort.Snapshot(NOW, NOW, true));
		worker.process(operations().get(0).getLogoutId()); verify(port, never()).revoke(any(), any());
		assertThat(operations().get(0).getStatus()).isEqualTo(LogoutAllOperation.Status.RECONCILIATION_REQUIRED);
	}
	@Test void readFailureRetriesWithoutSendingMutation() {
		accept("first"); when(port.inspect(any())).thenThrow(new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.READ_TRANSIENT));
		worker.process(operations().get(0).getLogoutId()); verify(port, never()).revoke(any(), any());
		var op = operations().get(0); assertThat(op.getStatus()).isEqualTo(LogoutAllOperation.Status.RETRY_WAIT);
		assertThat(op.getNextAttemptAt()).isAfter(NOW).isBeforeOrEqualTo(NOW.plusSeconds(5));
	}
	@Test void acknowledgedMutationReadRetryDoesNotRevokeTwice() {
		accept("first"); var id = operations().get(0).getLogoutId();
		when(port.inspect(any())).thenReturn(new FirebaseSessionRevocationPort.Snapshot(NOW.minusSeconds(3600), NOW, false))
				.thenThrow(new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.READ_TRANSIENT))
				.thenReturn(new FirebaseSessionRevocationPort.Snapshot(NOW.minusSeconds(3600), NOW, false));
		worker.process(id); time.set(NOW.plusSeconds(120)); worker.process(id);
		assertThat(operation(id).getStatus()).isEqualTo(LogoutAllOperation.Status.COMPLETED);
		verify(port, times(1)).revoke(any(), any());
	}
	@Test void internalOnlyLogoutCompletesWithoutFirebase() {
		when(identities.findByUserId(USER)).thenReturn(Optional.empty()); accept("local");
		var op = operations().get(0); assertThat(op.getStatus()).isEqualTo(LogoutAllOperation.Status.COMPLETED);
		assertThat(op.getCleanupAt()).isEqualTo(NOW.plus(Duration.ofDays(7))); verifyNoInteractions(port);
	}
	@Test void supersedeOnlyUndispatchedTasksDuringWithdrawal() {
		accept("first"); security.handoffWithdrawal(USER, NOW);
		assertThat(operations().get(0).getStatus()).isEqualTo(LogoutAllOperation.Status.SUPERSEDED_BY_WITHDRAWAL);
		assertThat(security.hasUnresolvedLogout(USER)).isFalse();
		worker.process(operations().get(0).getLogoutId()); verifyNoInteractions(port);
	}
	@Test void unresolvedActorBlocksReleaseButNotFreshLogin() {
		accept("first"); var op = operations().get(0); op.startMutation(NOW.minusSeconds(3600), NOW); mongo.save(op);
		security.handoffWithdrawal(USER, NOW);
		assertError(() -> security.guardIdentityRelease(USER), AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING);
		assertThatCode(() -> security.checkAndTouch(firebaseSession(1, NOW.plusSeconds(2)), true)).doesNotThrowAnyException();
	}
	@Test void immutableAuthenticationEvidenceSurvivesMultipleRotations() {
		accept("first"); var session = firebaseSession(1, NOW.plusSeconds(2));
		var prepared = new PreparedRefreshSession("test-only", session); issuer.savePrepared(prepared);
		String id = UUID.randomUUID().toString(); issuer.issueRotated(session, id, NOW.plusSeconds(3));
		var rotated = mongo.findById(id, RefreshSession.class);
		assertThat(rotated.getAuthentication()).isEqualTo(session.getAuthentication());
		String next = UUID.randomUUID().toString(); issuer.issueRotated(rotated, next, NOW.plusSeconds(4));
		assertThat(mongo.findById(next, RefreshSession.class).getAuthentication()).isEqualTo(session.getAuthentication());
		assertThatThrownBy(() -> rotated.attachAuthentication(session.getAuthentication())).isInstanceOf(IllegalStateException.class);
	}
	@Test void legacyMissingEvidenceIsNotAssumedLocalAfterLogout() {
		accept("first"); var legacy = RefreshSession.create(USER, "hash", NOW, NOW.plusSeconds(600));
		assertError(() -> security.checkAndTouch(legacy, false), AuthErrorStatus.SESSION_LOGGED_OUT);
		assertError(() -> issuer.savePrepared(new PreparedRefreshSession("test", legacy)), AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE);
	}
	@Test void epochProtectsAgainstStaleWriterEvenIfPhysicalSessionNotMarked() {
		var stale = localSession(0); accept("first");
		assertError(() -> issuer.savePrepared(new PreparedRefreshSession("test", stale)), AuthErrorStatus.SESSION_LOGGED_OUT);
		assertThat(stale.isRevoked()).isFalse();
	}
	@Test void missingTypeIsRejectedInsteadOfDefaultingToMemberOrLocal() {
		assertThatThrownBy(() -> new SessionAuthentication(0, null, null, null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new SessionAuthentication(0, SessionAuthentication.Source.FIREBASE, null, NOW)).isInstanceOf(IllegalArgumentException.class);
	}
	@Test void controlVersionPreventsLostUpdates() {
		accept("first"); var one = control(); var two = control(); one.touch(); mongo.save(one); two.touch();
		assertThatThrownBy(() -> mongo.save(two)).isInstanceOf(OptimisticLockingFailureException.class);
	}
	@Test void uniqueRequestAndEpochIndexesAndTerminalTtlExist() {
		accept("first");
		assertThatThrownBy(() -> mongo.insert(LogoutAllOperation.create("first", USER, 99, binding, null, NOW, NOW.plusSeconds(30))))
				.isInstanceOf(DuplicateKeyException.class);
		assertThat(mongo.indexOps(LogoutAllOperation.class).getIndexInfo()).anySatisfy(index -> {
			assertThat(index.getName()).isEqualTo("ttl_logout_operations"); assertThat(index.getExpireAfter()).hasValue(Duration.ZERO);
		});
	}
	@Test void terminalRetentionAlsoCoversRequestValidity() {
		var op = LogoutAllOperation.create("first", USER, 1, binding, null, NOW, NOW.plus(Duration.ofDays(10)));
		assertThat(coordinator.cleanupAt(op, NOW)).isEqualTo(NOW.plus(Duration.ofDays(10)).plusSeconds(60));
	}
	@Test void withdrawnUserCannotCreateJobOrRefreshSession() {
		when(users.findById(USER).orElseThrow().getStatus()).thenReturn(UserStatus.WITHDRAWN);
		assertError(() -> accept("first"), AuthErrorStatus.ACCOUNT_WITHDRAWN);
		assertError(() -> security.checkAndTouch(localSession(0), true), AuthErrorStatus.ACCOUNT_WITHDRAWN);
		assertThat(operations()).isEmpty();
	}
	@Test void flagsDefaultOffAndInvalidCombinationsFail() {
		assertThat(properties.isFenceEnabled() || properties.isCaptureEnabled() || properties.isWorkerEnabled()).isFalse();
		properties.setWorkerEnabled(true); assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);
	}
	@Test void fractionalRevocationBoundaryIsConservativeAndMonotonic() {
		var control = new UserSessionControl(USER); control.logout(binding.getFirebaseIdentityId(), NOW);
		control.confirmRevocation(binding.getFirebaseIdentityId(), NOW.plusMillis(1100));
		control.confirmRevocation(binding.getFirebaseIdentityId(), NOW);
		assertThat(control.getConfirmedFirebaseRevocationBoundary()).isEqualTo(NOW.plusSeconds(2));
		assertError(() -> control.validate(1, new SessionAuthentication(1, SessionAuthentication.Source.FIREBASE,
				binding.getFirebaseIdentityId(), NOW.plusSeconds(2)), false), AuthErrorStatus.SESSION_LOGGED_OUT);
	}
	@Test void transactionFailureIsSafe503() {
		doThrow(new org.springframework.transaction.TransactionSystemException("test failure")).when(tx).execute(any());
		assertError(() -> accept("first"), AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE);
	}
	@Test void localLoginCapturesEpochBeforePasswordVerification() {
		var user = users.findById(USER).orElseThrow();
		when(user.getUserId()).thenReturn(USER);
		when(user.getAccountType()).thenReturn(web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType.MEMBER);
		when(user.getPasswordHash()).thenReturn("test-hash");
		when(users.findByNormalizedEmail("test@example.invalid")).thenReturn(Optional.of(user));
		var encoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
		when(encoder.matches("test-password", "test-hash")).thenAnswer(invocation -> { accept("concurrent"); return true; });
		var access = mock(web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer.class);
		new web.tosunsaeng.identity.support.SignedUserTokenFixture(NOW).delegate(access);
		var service = new web.tosunsaeng.identity.domain.auth.local.application.LoginService(users,
				new web.tosunsaeng.identity.domain.user.domain.EmailNormalizer(), encoder, access, issuer,
				new web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter());
		assertError(() -> service.login(new web.tosunsaeng.identity.domain.auth.local.dto.request.LoginRequest(
				"test@example.invalid", "test-password")), AuthErrorStatus.SESSION_LOGGED_OUT);
	}
	@Test void physicalMarkingDoesNotAffectValidLocalOrNewFirebaseSessions() {
		var stale = localSession(0); mongo.save(stale); accept("first");
		var local = localSession(1); mongo.save(local);
		var oldFirebase = firebaseSession(1, NOW.plusSeconds(2)); mongo.save(oldFirebase);
		var fresh = firebaseSession(1, NOW.plusSeconds(20)); mongo.save(fresh);
		time.set(NOW.plusSeconds(10)); worker.process(operations().get(0).getLogoutId());
		security.markInvalidSessionsBatch(NOW.plusSeconds(30));
		assertThat(mongo.findById(oldFirebase.getSessionId(), RefreshSession.class).isRevoked()).isTrue();
		assertThat(mongo.findById(local.getSessionId(), RefreshSession.class).isRevoked()).isFalse();
		assertThat(mongo.findById(fresh.getSessionId(), RefreshSession.class).isRevoked()).isFalse();
	}
	@Test void currentEpochLegacyFirebaseEvidenceCannotBypassConfirmedBoundary() {
		var control = new UserSessionControl(USER); control.confirmRevocation(binding.getFirebaseIdentityId(), NOW); mongo.save(control);
		var legacy = RefreshSession.create(USER, "legacy-hash", NOW, NOW.plusSeconds(3600));
		assertError(() -> security.checkAndTouch(legacy, false), AuthErrorStatus.SESSION_LOGGED_OUT);
	}
	private void accept(String fingerprint) { coordinator.accept(USER, fingerprint, NOW.plusSeconds(1800)); }
	private UserSessionControl control() { return mongo.findById(USER, UserSessionControl.class); }
	private List<LogoutAllOperation> operations() { return mongo.findAll(LogoutAllOperation.class); }
	private LogoutAllOperation operation(String id) { return mongo.findById(id, LogoutAllOperation.class); }
	private RefreshSession firebaseSession(long epoch, Instant authAt) {
		var session = RefreshSession.create(USER, UUID.randomUUID().toString(), NOW, NOW.plusSeconds(3600));
		session.attachAuthentication(new SessionAuthentication(epoch, SessionAuthentication.Source.FIREBASE, binding.getFirebaseIdentityId(), authAt)); return session;
	}
	private RefreshSession localSession(long epoch) {
		var session = RefreshSession.create(USER, UUID.randomUUID().toString(), NOW, NOW.plusSeconds(3600));
		session.attachAuthentication(new SessionAuthentication(epoch, SessionAuthentication.Source.LOCAL, null, null)); return session;
	}
	private void assertError(Runnable work, AuthErrorStatus expected) {
		assertThatThrownBy(work::run).isInstanceOfSatisfying(AuthException.class, error -> assertThat(error.getErrorCode()).isEqualTo(expected));
	}
}
