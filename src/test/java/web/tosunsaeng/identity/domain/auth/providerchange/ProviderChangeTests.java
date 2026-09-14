package web.tosunsaeng.identity.domain.auth.providerchange;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import com.mongodb.client.*;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.transaction.support.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.*;
import web.tosunsaeng.identity.domain.auth.federation.repository.*;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.domain.*;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

/** Mapping/CAS tests on ephemeral Mongo. Snapshot rollback is test-only, not replica-set evidence. */
class ProviderChangeTests {
	static final String USER = "00000000-0000-4000-8000-000000000001";
	static final String KEY = "11111111-1111-4111-8111-111111111111";
	static final Instant NOW = Instant.parse("2026-09-11T00:00:00Z");
	MongoServer server; MongoClient client; MongoTemplate mongo;
	FirebaseIdentityRepository identities; SocialIdentityRepository socials; UserRepository users; User user;
	FirebaseIdentity binding; SessionSecurityService security; ProviderChangeGuard guard; ProviderChangeService service;
	ProviderLinkService links;
	ProviderUnlinkWorker worker; FirebaseProviderMutationPort port; ProviderChangeProperties properties;
	AtomicReference<Instant> time; AtomicReference<VerifiedFirebasePrincipal> proof;
	AtomicReference<FirebaseProviderMutationPort.Snapshot> remote; TransactionTemplate tx;

	@BeforeEach void setup() {
		server = new MongoServer(new MemoryBackend()); var address = server.bind();
		client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
		mongo = new MongoTemplate(client, "provider-change-test");
		for (var type : List.of(ProviderUnlinkOperation.class, ProviderRelinkAttempt.class, ProviderLinkAttempt.class, AuthMethodChangeControl.class,
				UserSessionControl.class, LogoutAllOperation.class, FirebaseIdentity.class, SocialIdentity.class)) {
			mongo.createCollection(type);
			new MongoPersistentEntityIndexResolver(mongo.getConverter().getMappingContext()).resolveIndexFor(type)
					.forEach(index -> mongo.indexOps(type).ensureIndex(index));
		}
		var factory = new MongoRepositoryFactory(mongo);
		identities = factory.getRepository(FirebaseIdentityRepository.class); socials = factory.getRepository(SocialIdentityRepository.class);
		binding = identities.save(FirebaseIdentity.create("test-project", "test-uid", USER, NOW.minusSeconds(3600)));
		for (var p : SocialProvider.values()) socials.save(SocialIdentity.create(USER, p, "subject-" + p, NOW.minusSeconds(60)));
		user = mock(User.class); users = mock(UserRepository.class);
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE); when(user.isMember()).thenReturn(true);
		when(users.findById(USER)).thenReturn(Optional.of(user));
		time = new AtomicReference<>(NOW);
		Clock clock = new Clock() {
			public ZoneId getZone() { return ZoneOffset.UTC; }
			public Clock withZone(ZoneId zone) { return this; }
			public Instant instant() { return time.get(); }
		};
		tx = mock(TransactionTemplate.class);
		when(tx.execute(any())).thenAnswer(invocation -> {
			Map<String, List<Document>> snapshot = new HashMap<>();
			for (String name : mongo.getDb().listCollectionNames()) snapshot.put(name, mongo.getCollection(name).find().into(new ArrayList<>()));
			try { return ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(new SimpleTransactionStatus()); }
			catch (RuntimeException e) {
				for (String name : mongo.getDb().listCollectionNames()) {
					var collection = mongo.getCollection(name); collection.deleteMany(new Document());
					var rows = snapshot.getOrDefault(name, List.of()); if (!rows.isEmpty()) collection.insertMany(rows);
				}
				throw e;
			}
		});
		guard = new ProviderChangeGuard(mongo); security = new SessionSecurityService(mongo, tx, users, identities); security.setProviderChanges(guard);
		properties = new ProviderChangeProperties(); properties.setFenceEnabled(true); properties.setEnabled(true); properties.setLinkEnabled(true);
		proof = new AtomicReference<>(principal(SocialProvider.APPLE, NOW.minusSeconds(10), EnumSet.allOf(SocialProvider.class)));
		var verifier = mock(FirebaseAuthenticationVerifier.class); when(verifier.verify(anyString(), any())).thenAnswer(i -> proof.get());
		service = new ProviderChangeService(mongo, tx, security, guard, verifier, identities, socials, users, properties, clock, null);
		links = new ProviderLinkService(service, mongo, security, guard, socials, properties, clock);
		Map<SocialProvider, String> methods = new EnumMap<>(SocialProvider.class);
		for (var p : SocialProvider.values()) methods.put(p, "subject-" + p);
		remote = new AtomicReference<>(new FirebaseProviderMutationPort.Snapshot(NOW.minusSeconds(3600), NOW.minusSeconds(100), false, methods));
		port = mock(FirebaseProviderMutationPort.class);
		when(port.inspect(any())).thenAnswer(i -> remote.get());
		doAnswer(i -> {
			var r = remote.get(); var m = new EnumMap<SocialProvider, String>(SocialProvider.class); m.putAll(r.providers()); m.remove(i.getArgument(1));
			remote.set(new FirebaseProviderMutationPort.Snapshot(r.createdAt(), r.validAfter(), false, m)); return null;
		}).when(port).unlink(any(), any());
		doAnswer(i -> { var r = remote.get(); remote.set(new FirebaseProviderMutationPort.Snapshot(r.createdAt(), i.getArgument(1), false, r.providers())); return null; }).when(port).revoke(any(), any());
		worker = new ProviderUnlinkWorker(mongo, security, identities, socials, port, properties, null, clock);
	}
	@AfterEach void close() { client.close(); server.shutdownNow(); }
	VerifiedFirebasePrincipal principal(SocialProvider method, Instant auth, Set<SocialProvider> linked) {
		Set<FirebaseAuthenticationMethod> methods = EnumSet.of(FirebaseAuthenticationMethod.PHONE);
		linked.forEach(p -> methods.add(FirebaseAuthenticationMethod.valueOf(p.name())));
		return new VerifiedFirebasePrincipal("test-project", "test-uid", FirebaseAuthenticationMethod.valueOf(method.name()),
				auth, time.get(), time.get().plusSeconds(3600), true, true, "+821012345678", methods,
				linked.stream().map(p -> new VerifiedSocialPrincipal(p, "subject-" + p)).toList());
	}
	ProviderChangeService.Status accept() { return service.unlink(USER, SocialProvider.GOOGLE, "test-proof", List.of(KEY)); }
	ProviderUnlinkOperation op(String id) { return mongo.findById(id, ProviderUnlinkOperation.class); }
	void advance() { time.set(time.get().plusSeconds(2)); proof.set(principal(SocialProvider.APPLE, time.get(), EnumSet.allOf(SocialProvider.class))); }
	void complete(String id) { worker.process(id); assertThat(op(id).getState()).isEqualTo(ProviderUnlinkOperation.State.COMPLETED); }
	ProviderLinkService.Status prepare() {
		advance(); proof.set(principal(SocialProvider.APPLE, time.get(), EnumSet.of(SocialProvider.APPLE, SocialProvider.KAKAO)));
		var prepared = links.prepare(USER, SocialProvider.GOOGLE, "test-proof", List.of(KEY));
		return links.start(USER, prepared.linkAttemptId(), "test-proof");
	}

	@ParameterizedTest @EnumSource(SocialProvider.class) void eachProviderCanBeRemovedUsingAnother(SocialProvider target) {
		SocialProvider remaining = target == SocialProvider.APPLE ? SocialProvider.KAKAO : SocialProvider.APPLE;
		proof.set(principal(remaining, NOW.minusSeconds(1), EnumSet.allOf(SocialProvider.class)));
		var result = service.unlink(USER, target, "proof", List.of(KEY)); complete(result.operationId());
		assertThat(socials.findAllByUserId(USER)).hasSize(2);
		assertThat(identities.findByUserId(USER)).isPresent();
		assertThat(guard.control(USER, binding.getFirebaseIdentityId()).isBlocked(target)).isTrue();
		verify(port, times(1)).unlink(any(), eq(target)); verify(port, times(1)).revoke(any(), any());
	}
	@Test void duplicateRequestDoesNotRepeatEpochOrRemoteWork() {
		var first = accept(); var second = accept();
		assertThat(second.operationId()).isEqualTo(first.operationId());
		assertThat(security.captureEpoch(USER)).isEqualTo(1); assertThat(mongo.findAll(ProviderUnlinkOperation.class)).hasSize(1);
		verifyNoInteractions(port);
	}
	@Test void duplicateKeyDifferentTargetRejected() {
		accept(); assertThatThrownBy(() -> service.unlink(USER, SocialProvider.KAKAO, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
		assertThat(security.captureEpoch(USER)).isEqualTo(1);
	}
	@Test void lastMethodRejectedBeforeAnyWrites() {
		proof.set(principal(SocialProvider.GOOGLE, NOW.minusSeconds(1), EnumSet.of(SocialProvider.GOOGLE)));
		assertThatThrownBy(this::accept).hasMessageContaining("마지막");
		assertThat(mongo.findAll(ProviderUnlinkOperation.class)).isEmpty(); assertThat(security.captureEpoch(USER)).isZero();
	}
	@Test void targetMethodIsNotRemainingReauthentication() {
		proof.set(principal(SocialProvider.GOOGLE, NOW.minusSeconds(1), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(this::accept).hasMessageContaining("남는");
	}
	@Test void oldAuthenticationRejected() {
		proof.set(principal(SocialProvider.APPLE, NOW.minusSeconds(301), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(this::accept).hasMessageContaining("최근");
	}
	@Test void blockedGoogleCannotLogInButFreshAppleCan() {
		accept(); advance();
		assertThatThrownBy(() -> guard.authenticate(USER, binding.getFirebaseIdentityId(), FirebaseAuthenticationMethod.GOOGLE, time.get())).hasMessageContaining("재연결");
		guard.authenticate(USER, binding.getFirebaseIdentityId(), FirebaseAuthenticationMethod.APPLE, time.get());
	}
	@Test void oldEpochCannotIssueAfterAccept() {
		accept(); var session = RefreshSession.create(USER, "test-hash", NOW, NOW.plusSeconds(3600));
		session.attachAuthentication(new SessionAuthentication(0, SessionAuthentication.Source.FIREBASE, binding.getFirebaseIdentityId(), NOW, FirebaseAuthenticationMethod.APPLE));
		assertThatThrownBy(() -> security.checkAndTouch(session, true)).isInstanceOf(AuthException.class);
	}
	@Test void statusUsesOwnerProofAndDoesNotMutate() {
		var first = accept(); advance();
		assertThat(service.status(KEY, "proof").operationId()).isEqualTo(first.operationId());
		assertThat(security.captureEpoch(USER)).isEqualTo(1); verifyNoInteractions(port);
	}
	@Test void statusRejectsOldAuthenticationBoundary() {
		accept(); assertThatThrownBy(() -> service.status(KEY, "proof")).isInstanceOf(AuthException.class);
	}
	@Test void otherOwnerCannotLookupStatus() {
		accept(); var p = proof.get();
		proof.set(new VerifiedFirebasePrincipal(p.firebaseProjectId(), "another-uid", p.signInMethod(), p.authTime(), p.issuedAt(), p.expiresAt(), true, true, p.verifiedPhoneNumber(), p.linkedMethods(), p.linkedSocialPrincipals()));
		assertThatThrownBy(() -> service.status(KEY, "proof")).isInstanceOf(AuthException.class);
	}
	@Test void failureAfterDispatchDoesNotRedispatchOrExpireEvidence() {
		var first = accept(); doThrow(new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.RESULT_UNKNOWN)).when(port).unlink(any(), any());
		worker.process(first.operationId()); worker.process(first.operationId());
		assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED);
		assertThat(op(first.operationId()).getCleanupAt()).isNull(); verify(port, times(1)).unlink(any(), any()); verify(port, never()).revoke(any(), any());
		assertThat(security.hasUnresolvedLogout(USER)).isTrue();
	}
	@Test void deadWorkerAfterStartIsUnknownNotRetried() {
		var first = accept(); var op = op(first.operationId());
		op.start(ProviderUnlinkOperation.State.UNLINK_STARTED, remote.get().createdAt(), NOW); mongo.save(op);
		worker.process(op.getOperationId()); assertThat(op(op.getOperationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED);
		verifyNoInteractions(port);
	}
	@Test void readFailureIsSafelyRetried() {
		var first = accept(); when(port.inspect(any())).thenThrow(new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.READ_TRANSIENT));
		worker.process(first.operationId()); assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.PENDING);
		assertThat(op(first.operationId()).getAttemptCount()).isEqualTo(1); verify(port, never()).unlink(any(), any());
	}
	@Test void remoteLostRemainingMethodCannotBeCompleted() {
		var first = accept(); remote.set(new FirebaseProviderMutationPort.Snapshot(remote.get().createdAt(), NOW, false, Map.of(SocialProvider.GOOGLE, "subject-GOOGLE")));
		worker.process(first.operationId()); assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED);
		verify(port, never()).unlink(any(), any());
	}
	@Test void changedBindingNeverTargetsReplacement() {
		var first = accept(); identities.delete(binding); identities.save(FirebaseIdentity.create("test-project", "replacement", USER, NOW));
		worker.process(first.operationId()); assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED); verifyNoInteractions(port);
	}
	@Test void oldSyncCannotResurrectEvenAfterOperationHistoryDeleted() {
		var first = accept(); complete(first.operationId()); mongo.remove(op(first.operationId())); advance();
		Runnable save = mock(Runnable.class);
		assertThatThrownBy(() -> service.synchronize(USER, 1, service.captureRevision(USER), proof.get(), null, save)).isInstanceOf(AuthException.class);
		verifyNoInteractions(save);
	}
	@Test void explicitRelinkConsumesOnceAndPreservesUser() {
		var first = accept(); complete(first.operationId()); var permit = prepare(); advance();
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		Runnable save = () -> socials.save(SocialIdentity.create(USER, SocialProvider.GOOGLE, "subject-GOOGLE", time.get()));
		links.complete(USER, permit.linkAttemptId(), "proof");
		assertThat(guard.control(USER, binding.getFirebaseIdentityId()).isBlocked(SocialProvider.GOOGLE)).isFalse();
		Runnable replaySave = mock(Runnable.class);
		links.complete(USER, permit.linkAttemptId(), "proof"); verifyNoInteractions(replaySave);
		assertThat(identities.findByUserId(USER).orElseThrow().getFirebaseIdentityId()).isEqualTo(binding.getFirebaseIdentityId());
		assertThatThrownBy(() -> guard.authenticate(USER, binding.getFirebaseIdentityId(), FirebaseAuthenticationMethod.GOOGLE, NOW)).hasMessageContaining("최근");
	}
	@Test void expiredPermitIsNotAutomaticallyReleasedBecauseClientMayHaveLinked() {
		var first = accept(); complete(first.operationId()); var permit = prepare(); time.set(time.get().plusSeconds(301));
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.complete(USER, permit.linkAttemptId(), "proof")).hasMessageContaining("만료");
		assertThat(security.hasUnresolvedLogout(USER)).isTrue(); assertThat(mongo.findById(permit.linkAttemptId(), ProviderLinkAttempt.class).getCleanupAt()).isNull();
	}
	@Test void relinkCannotBeginBeforeUnlinkCompleted() {
		accept(); advance();
		assertThatThrownBy(() -> links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
	}
	@Test void proofBeforePrepareCannotConsumePermit() {
		var first = accept(); complete(first.operationId()); var permit = prepare();
		proof.set(principal(SocialProvider.GOOGLE, NOW, EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.complete(USER, permit.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
	}
	@Test void withdrawalSupersedesOnlyActorsThatCannotStillDispatch() {
		var first = accept(); security.handoffWithdrawal(USER, NOW);
		assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.SUPERSEDED);
		assertThat(security.hasUnresolvedLogout(USER)).isFalse(); worker.process(first.operationId()); verifyNoInteractions(port);
	}
	@Test void withdrawalKeepsInFlightActorAsReleaseBarrier() {
		var first = accept(); var op = op(first.operationId()); op.start(ProviderUnlinkOperation.State.UNLINK_STARTED, NOW, NOW); mongo.save(op);
		security.handoffWithdrawal(USER, NOW);
		assertThat(security.hasUnresolvedLogout(USER)).isTrue(); assertThatThrownBy(() -> security.guardIdentityRelease(USER)).isInstanceOf(AuthException.class);
	}
	@Test void commonSlotPreventsOverlappingLogoutActor() {
		var control = new UserSessionControl(USER); control.claimLogout("existing-logout"); mongo.save(control);
		assertThatThrownBy(this::accept).isInstanceOf(AuthException.class); assertThat(security.captureEpoch(USER)).isZero();
	}
	@Test void operationCasRejectsStaleWriter() {
		var first = accept(); var a = op(first.operationId()); var b = op(first.operationId()); a.claim("a", NOW.plusSeconds(60)); mongo.save(a);
		b.claim("b", NOW.plusSeconds(60)); assertThatThrownBy(() -> mongo.save(b)).isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
	}
	@Test void disabledNewCaptureDoesNotDisableStatusOrBlocks() {
		var first = accept(); properties.setEnabled(false); advance();
		assertThat(service.status(KEY, "proof").operationId()).isEqualTo(first.operationId());
		assertThatThrownBy(this::accept).isInstanceOf(AuthException.class);
		assertThatThrownBy(() -> guard.authenticate(USER, binding.getFirebaseIdentityId(), FirebaseAuthenticationMethod.GOOGLE, time.get())).isInstanceOf(AuthException.class);
	}
	@Test void keysAreStrictAndProofIsNotSerializedByDtos() {
		for (var key : List.of("bad", "11111111-1111-1111-8111-111111111111", KEY.toUpperCase() + ",")) {
			assertThatThrownBy(() -> ProviderChangeService.requestHash(List.of(key))).isInstanceOf(AuthException.class);
		}
		assertThatThrownBy(() -> ProviderChangeService.requestHash(List.of(KEY, KEY))).isInstanceOf(AuthException.class);
		assertThat(new ProviderChangeController.ChangeRequest(SocialProvider.GOOGLE, "do-not-print").toString()).doesNotContain("do-not-print");
	}
	@Test void lateSyncFromBeforeRelinkCannotRestoreAnOldSubject() {
		var first = accept(); complete(first.operationId()); var permit = prepare();
		long oldRevision = service.captureRevision(USER); var stale = proof.get(); advance();
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		links.complete(USER, permit.linkAttemptId(), "proof");
		var save = mock(Runnable.class);
		assertThatThrownBy(() -> service.synchronize(USER, 1, oldRevision, stale, null, save)).isInstanceOf(AuthException.class);
		verifyNoInteractions(save);
	}
	@Test void acceptanceInsertFailureRollsBackEpochBlockAndSlot() {
		var writer = spy(mongo);
		doThrow(new org.springframework.dao.DataAccessResourceFailureException("test-only failure"))
				.when(writer).insert(any(ProviderUnlinkOperation.class));
		var verifier = mock(FirebaseAuthenticationVerifier.class); when(verifier.verify(anyString(), any())).thenReturn(proof.get());
		var failing = new ProviderChangeService(writer, tx, security, guard, verifier, identities, socials, users, properties,
				Clock.fixed(NOW, ZoneOffset.UTC), null);
		assertThatThrownBy(() -> failing.unlink(USER, SocialProvider.GOOGLE, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
		assertThat(security.captureEpoch(USER)).isZero(); assertThat(security.control(USER).getActiveLogoutId()).isNull();
		assertThat(guard.hasState(USER)).isFalse(); assertThat(mongo.findAll(ProviderUnlinkOperation.class)).isEmpty();
	}
	@Test void lateUnlinkAcknowledgementCannotAdvanceAfterLeaseWasReconciled() {
		var first = accept();
		doAnswer(i -> {
			time.set(NOW.plusSeconds(61)); worker.process(first.operationId()); return null;
		}).when(port).unlink(any(), any());
		worker.process(first.operationId());
		assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED);
		verify(port, times(1)).unlink(any(), any()); verify(port, never()).revoke(any(), any());
		assertThat(security.control(USER).getActiveLogoutId()).isNotNull();
	}
	@Test void unknownRevokeObservesBoundaryWithoutReleasingRemoteActor() {
		var first = accept(); time.set(NOW.plusSeconds(10));
		doAnswer(i -> {
			var r = remote.get(); remote.set(new FirebaseProviderMutationPort.Snapshot(r.createdAt(), time.get(), false, r.providers()));
			throw new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.RESULT_UNKNOWN);
		}).when(port).revoke(any(), any());
		worker.process(first.operationId()); worker.process(first.operationId());
		assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED);
		assertThat(security.control(USER).getConfirmedFirebaseRevocationBoundary()).isEqualTo(time.get());
		var session = RefreshSession.create(USER, "test-only-hash", NOW, NOW.plusSeconds(3600));
		session.attachAuthentication(new SessionAuthentication(1, SessionAuthentication.Source.FIREBASE,
				binding.getFirebaseIdentityId(), NOW.plusSeconds(2), FirebaseAuthenticationMethod.APPLE));
		assertThatThrownBy(() -> security.checkAndTouch(session, false)).isInstanceOf(AuthException.class);
		verify(port, times(1)).revoke(any(), any()); assertThat(security.captureEpoch(USER)).isEqualTo(1);
	}
	@Test void localFinalizationFailureDoesNotRepeatSuccessfulRemoteMutations() {
		var first = accept(); var writer = spy(mongo);
		doThrow(new org.springframework.dao.DataAccessResourceFailureException("test-only failure"))
				.when(writer).remove(any(org.springframework.data.mongodb.core.query.Query.class), eq(SocialIdentity.class));
		var failing = new ProviderUnlinkWorker(writer, security, identities, socials, port, properties, null, Clock.fixed(NOW, ZoneOffset.UTC));
		failing.process(first.operationId()); failing.process(first.operationId());
		assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED);
		assertThat(socials.findAllByUserId(USER)).hasSize(3); assertThat(op(first.operationId()).getCleanupAt()).isNull();
		verify(port, times(1)).unlink(any(), any()); verify(port, times(1)).revoke(any(), any());
	}
	@Test void phoneProofCannotAuthorizeUnlink() {
		var p = proof.get(); proof.set(new VerifiedFirebasePrincipal(p.firebaseProjectId(), p.firebaseUid(), FirebaseAuthenticationMethod.PHONE,
				p.authTime(), p.issuedAt(), p.expiresAt(), true, true, p.verifiedPhoneNumber(), p.linkedMethods(), p.linkedSocialPrincipals()));
		assertThatThrownBy(this::accept).hasMessageContaining("남는"); assertThat(security.captureEpoch(USER)).isZero();
	}
	@Test void changedUidOnSameBindingIdIsNeverMutated() {
		var first = accept(); org.springframework.test.util.ReflectionTestUtils.setField(binding, "firebaseUid", "replacement-uid"); identities.save(binding);
		worker.process(first.operationId()); verifyNoInteractions(port);
		assertThat(op(first.operationId()).getState()).isEqualTo(ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED);
	}
	void removeForFirstLink(SocialProvider target) {
		socials.findAllByUserId(USER).stream().filter(s -> s.getProvider() == target).forEach(socials::delete);
		var remaining = EnumSet.noneOf(SocialProvider.class); socials.findAllByUserId(USER).forEach(s -> remaining.add(s.getProvider()));
		proof.set(principal(remaining.iterator().next(), time.get(), remaining));
	}
	ProviderLinkService.Status firstPrepare(SocialProvider target) {
		removeForFirstLink(target);
		return links.prepare(USER, target, "proof", List.of(KEY));
	}
	@ParameterizedTest @EnumSource(SocialProvider.class)
	void firstConnectionUsesSameProtocolForAllProviders(SocialProvider target) {
		var p = firstPrepare(target);
		assertThat(p.status()).isEqualTo("PREPARED"); assertThat(p.linkAllowed()).isFalse();
		assertThat(security.control(USER).getActiveLogoutId()).isNull();
		assertThat(security.hasUnresolvedLogout(USER)).isFalse();
		var started = links.start(USER, p.linkAttemptId(), "proof");
		assertThat(started.linkAllowed()).isTrue();
		assertThat(security.hasUnresolvedLogout(USER)).isTrue();
		assertThatThrownBy(() -> guard.authenticate(USER, binding.getFirebaseIdentityId(), FirebaseAuthenticationMethod.valueOf(target.name()), time.get())).isInstanceOf(AuthException.class);
		time.set(time.get().plusSeconds(2)); proof.set(principal(target, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThat(links.complete(USER, p.linkAttemptId(), "proof").status()).isEqualTo("COMPLETED");
		assertThat(links.complete(USER, p.linkAttemptId(), "proof").linkAllowed()).isFalse();
		assertThat(socials.findAllByUserId(USER)).hasSize(3); assertThat(security.captureEpoch(USER)).isZero();
		assertThat(security.hasUnresolvedLogout(USER)).isFalse(); assertThat(security.control(USER).getActiveLogoutId()).isNull();
		guard.validatePrincipal(proof.get());
	}
	@Test void abandonedPrepareExpiresWithoutBlockingIdentityRelease() {
		var p = firstPrepare(SocialProvider.GOOGLE);
		time.set(NOW.plusSeconds(301)); proof.set(principal(SocialProvider.APPLE, time.get(), EnumSet.of(SocialProvider.APPLE, SocialProvider.KAKAO)));
		assertThat(links.status(USER, KEY, "proof").status()).isEqualTo("EXPIRED");
		assertThatThrownBy(() -> links.start(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		assertThat(security.hasUnresolvedLogout(USER)).isFalse(); security.guardIdentityRelease(USER);
		assertThat(mongo.findById(p.linkAttemptId(), ProviderLinkAttempt.class).getCleanupAt()).isNotNull();
		assertThat(links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(UUID.randomUUID().toString())).status()).isEqualTo("PREPARED");
	}
	@Test void prepareResponseLossRecoversByOriginalRequestKey() {
		var p = firstPrepare(SocialProvider.GOOGLE);
		assertThat(links.status(USER, KEY, "proof").linkAttemptId()).isEqualTo(p.linkAttemptId());
		assertThat(links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(KEY)).linkAttemptId()).isEqualTo(p.linkAttemptId());
		assertThat(mongo.findAll(ProviderLinkAttempt.class)).hasSize(1);
		assertThatThrownBy(() -> links.prepare(USER, SocialProvider.KAKAO, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
	}
	@Test void repeatedStartNeverGrantsAnotherFirebaseInvocation() {
		var p = firstPrepare(SocialProvider.GOOGLE);
		assertThat(links.start(USER, p.linkAttemptId(), "proof").linkAllowed()).isTrue();
		assertThat(links.start(USER, p.linkAttemptId(), "proof").linkAllowed()).isFalse();
		assertThat(links.status(USER, KEY, "proof").linkAllowed()).isFalse();
		assertThat(service.captureRevision(USER)).isEqualTo(1);
	}
	@Test void completeWithoutStartIsRejected() {
		var p = firstPrepare(SocialProvider.GOOGLE); advance();
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.complete(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		assertThat(socials.findAllByUserId(USER)).hasSize(2);
	}
	@Test void outOfBandFirebaseLinkCannotPrepareSyncOrAuthenticate() {
		removeForFirstLink(SocialProvider.GOOGLE);
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
		assertThatThrownBy(() -> guard.validatePrincipal(proof.get())).isInstanceOf(AuthException.class);
		var save = mock(Runnable.class);
		assertThatThrownBy(() -> service.synchronize(USER, 0, 0, proof.get(), null, save)).isInstanceOf(AuthException.class);
		verifyNoInteractions(save);
	}
	@Test void alreadyLinkedIsNoOpWithoutFirebaseGrant() {
		var result = links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(KEY));
		assertThat(result.status()).isEqualTo("ALREADY_LINKED"); assertThat(result.linkAllowed()).isFalse();
		assertThat(mongo.findAll(ProviderLinkAttempt.class)).hasSize(1); assertThat(security.hasUnresolvedLogout(USER)).isFalse();
		assertThat(links.status(USER, KEY, "proof").status()).isEqualTo("ALREADY_LINKED");
		assertThatThrownBy(() -> links.prepare(USER, SocialProvider.APPLE, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
	}
	@Test void completingTargetDoesNotSaveUnrelatedRemoteIdentity() {
		removeForFirstLink(SocialProvider.KAKAO);
		var p = firstPrepare(SocialProvider.GOOGLE);
		proof.set(principal(SocialProvider.APPLE, time.get(), EnumSet.of(SocialProvider.APPLE)));
		links.start(USER, p.linkAttemptId(), "proof"); advance();
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		links.complete(USER, p.linkAttemptId(), "proof");
		assertThat(socials.findAllByUserId(USER)).extracting(SocialIdentity::getProvider).containsExactlyInAnyOrder(SocialProvider.APPLE, SocialProvider.GOOGLE);
		proof.set(principal(SocialProvider.KAKAO, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> guard.validatePrincipal(proof.get())).isInstanceOf(AuthException.class);
	}
	@Test void newCaptureOffStillAllowsStartedCompletionAndStatus() {
		var p = firstPrepare(SocialProvider.GOOGLE); links.start(USER, p.linkAttemptId(), "proof");
		properties.setLinkEnabled(false);
		assertThatThrownBy(() -> links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
		advance(); proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThat(links.complete(USER, p.linkAttemptId(), "proof").status()).isEqualTo("COMPLETED");
		assertThat(links.status(USER, KEY, "proof").status()).isEqualTo("COMPLETED");
	}
	@Test void preparedBeforeLogoutCannotStartAfterEpochChanges() {
		var p = firstPrepare(SocialProvider.GOOGLE);
		var c = security.control(USER); c.logout(binding.getFirebaseIdentityId(), NOW); mongo.save(c); advance();
		proof.set(principal(SocialProvider.APPLE, time.get(), EnumSet.of(SocialProvider.APPLE, SocialProvider.KAKAO)));
		assertThatThrownBy(() -> links.start(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		assertThat(links.status(USER, KEY, "proof").status()).isEqualTo("SUPERSEDED");
	}
	@Test void secondPreparedAttemptCannotStartDuringOrAfterFirst() {
		var p = firstPrepare(SocialProvider.GOOGLE);
		var other = links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(UUID.randomUUID().toString()));
		links.start(USER, p.linkAttemptId(), "proof");
		assertThatThrownBy(() -> links.start(USER, other.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		advance(); proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		links.complete(USER, p.linkAttemptId(), "proof");
		proof.set(principal(SocialProvider.APPLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.start(USER, other.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
	}
	@Test void legacyUnconsumedAttemptNeverBecomesExpirablePrepared() {
		removeForFirstLink(SocialProvider.GOOGLE);
		mongo.insert(ProviderRelinkAttempt.create(USER, binding.getFirebaseIdentityId(), SocialProvider.GOOGLE, 0, NOW.minusSeconds(900), NOW.minusSeconds(600)));
		assertThat(security.hasUnresolvedLogout(USER)).isTrue();
		assertThatThrownBy(() -> links.prepare(USER, SocialProvider.GOOGLE, "proof", List.of(KEY))).isInstanceOf(AuthException.class);
		assertThatThrownBy(() -> service.prepare(USER, SocialProvider.GOOGLE, "proof")).isInstanceOf(AuthException.class);
	}
	@Test void startedLinkRemainsWithdrawalReleaseBarrier() {
		var p = firstPrepare(SocialProvider.GOOGLE); links.start(USER, p.linkAttemptId(), "proof");
		security.handoffWithdrawal(USER, NOW);
		assertThatThrownBy(() -> security.guardIdentityRelease(USER)).isInstanceOf(AuthException.class);
		assertThat(mongo.findById(p.linkAttemptId(), ProviderLinkAttempt.class).getCleanupAt()).isNull();
	}
	@Test void changedBindingOrOwnerCannotCompleteOrRead() {
		var p = firstPrepare(SocialProvider.GOOGLE); links.start(USER, p.linkAttemptId(), "proof"); advance();
		assertThatThrownBy(() -> links.status("00000000-0000-4000-8000-000000000099", KEY, "proof")).isInstanceOf(AuthException.class);
		org.springframework.test.util.ReflectionTestUtils.setField(binding, "createdAt", NOW); identities.save(binding);
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.complete(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
	}
	@Test void completionFailureRollsBackTargetSaveReleaseAndReceipt() {
		var p = firstPrepare(SocialProvider.GOOGLE); links.start(USER, p.linkAttemptId(), "proof"); advance();
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		var writer = spy(mongo);
		doThrow(new org.springframework.dao.DataAccessResourceFailureException("test failure")).when(writer).save(any(ProviderLinkAttempt.class));
		var failing = new ProviderLinkService(service, writer, security, guard, socials, properties, Clock.fixed(time.get(), ZoneOffset.UTC));
		assertThatThrownBy(() -> failing.complete(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		assertThat(socials.findAllByUserId(USER)).hasSize(2);
		assertThat(guard.control(USER, binding.getFirebaseIdentityId()).isBlocked(SocialProvider.GOOGLE)).isTrue();
		assertThat(mongo.findById(p.linkAttemptId(), ProviderLinkAttempt.class).getState()).isEqualTo(ProviderLinkAttempt.State.STARTED);
		assertThat(security.control(USER).getActiveLogoutId()).isNotNull();
	}
	@Test void foreignTargetOwnerUsesWithdrawalGateWithoutSavingOrReleasing() {
		var p = firstPrepare(SocialProvider.GOOGLE); links.start(USER, p.linkAttemptId(), "proof"); advance();
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		String other = "00000000-0000-4000-8000-000000000099";
		socials.save(SocialIdentity.create(other, SocialProvider.GOOGLE, "subject-GOOGLE", NOW));
		var gate = mock(WithdrawalEnrollmentGate.class); links.setWithdrawalGate(gate);
		doThrow(new AuthException(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING)).when(gate).checkExistingOwner(other);
		assertThatThrownBy(() -> links.complete(USER, p.linkAttemptId(), "proof")).isInstanceOfSatisfying(AuthException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING));
		assertThat(socials.findAllByUserId(USER)).hasSize(2); assertThat(security.hasUnresolvedLogout(USER)).isTrue();
	}
	@Test void delayedCompletedResponseCannotUndoLaterUnlink() {
		var p = firstPrepare(SocialProvider.GOOGLE); links.start(USER, p.linkAttemptId(), "proof"); advance();
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		links.complete(USER, p.linkAttemptId(), "proof"); advance();
		var unlink = accept(); complete(unlink.operationId()); advance();
		proof.set(principal(SocialProvider.APPLE, time.get(), EnumSet.of(SocialProvider.APPLE, SocialProvider.KAKAO)));
		assertThat(links.status(USER, KEY, "proof").status()).isEqualTo("SUPERSEDED");
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.complete(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		assertThat(guard.control(USER, binding.getFirebaseIdentityId()).isBlocked(SocialProvider.GOOGLE)).isTrue();
	}
	@Test void startFailureRollsBackGrantSlotAndTargetBlock() {
		var p = firstPrepare(SocialProvider.GOOGLE);
		var writer = spy(mongo);
		doThrow(new org.springframework.dao.DataAccessResourceFailureException("test failure")).when(writer).save(any(ProviderLinkAttempt.class));
		var failing = new ProviderLinkService(service, writer, security, guard, socials, properties, Clock.fixed(time.get(), ZoneOffset.UTC));
		assertThatThrownBy(() -> failing.start(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		assertThat(security.control(USER).getActiveLogoutId()).isNull(); assertThat(guard.hasState(USER)).isFalse();
		assertThat(mongo.findById(p.linkAttemptId(), ProviderLinkAttempt.class).getState()).isEqualTo(ProviderLinkAttempt.State.PREPARED);
	}
	@Test void onlyPostStartTargetProofCanComplete() {
		var p = firstPrepare(SocialProvider.GOOGLE); links.start(USER, p.linkAttemptId(), "proof");
		assertThatThrownBy(() -> links.complete(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		proof.set(principal(SocialProvider.GOOGLE, time.get(), EnumSet.allOf(SocialProvider.class)));
		assertThatThrownBy(() -> links.complete(USER, p.linkAttemptId(), "proof")).isInstanceOf(AuthException.class);
		assertThat(socials.findAllByUserId(USER)).hasSize(2);
	}
	@Test void oldAndNewSessionEvidenceRoundTripMongo() {
		for (var method : new FirebaseAuthenticationMethod[]{null, FirebaseAuthenticationMethod.APPLE}) {
			var session = RefreshSession.create(USER, "test-hash-" + method, NOW, NOW.plusSeconds(3600));
			session.attachAuthentication(new SessionAuthentication(0, SessionAuthentication.Source.FIREBASE, binding.getFirebaseIdentityId(), NOW, method));
			mongo.save(session);
			assertThat(mongo.findById(session.getSessionId(), RefreshSession.class).getAuthentication().firebaseMethod()).isEqualTo(method);
		}
	}
}
