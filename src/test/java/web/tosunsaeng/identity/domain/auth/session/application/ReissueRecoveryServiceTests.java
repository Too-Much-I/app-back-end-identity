package web.tosunsaeng.identity.domain.auth.session.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.dao.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.*;
import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.session.domain.*;
import web.tosunsaeng.identity.domain.auth.session.dto.request.*;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.*;
import web.tosunsaeng.identity.domain.auth.session.repository.*;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.*;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.*;
import web.tosunsaeng.identity.global.security.refresh.*;

/** Mock transaction snapshots verify application rollback wiring, NOT real Mongo transaction guarantees. */
class ReissueRecoveryServiceTests {
	static final String USER = "00000000-0000-4000-8000-000000000001";
	static final String ID = "11111111-1111-4111-8111-111111111111";
	static final String OTHER_ID = "22222222-2222-4222-8222-222222222222";
	static final Instant NOW = Instant.parse("2026-09-09T00:00:00Z");
	static final String RAW = "test-only-initial-refresh";
	Map<String, RefreshSession> db;
	Map<String, RefreshReissueResponse> payloads;
	ObjectMapper json;
	RefreshSessionRepository sessions;
	RefreshReissueResponseRepository responses;
	UserRepository users;
	User user;
	SessionSecurityService security;
	TransactionTemplate tx;
	RefreshSessionIssuer refreshIssuer;
	AccessTokenIssuer accessIssuer;
	RefreshTokenGenerator generator;
	RefreshTokenHasher hasher = new RefreshTokenHasher();
	ReissueRecoveryProperties properties;
	ReissueResponseCipher cipher;
	ReissueRecoveryService service;
	AtomicReference<Instant> time;
	Clock clock;
	String sourceId;
	SimpleMeterRegistry metrics;
	UserSessionControl control;

	@BeforeEach void setup() {
		db = new HashMap<>(); payloads = new HashMap<>();
		json = new ObjectMapper().findAndRegisterModules();
		json.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE).setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		time = new AtomicReference<>(NOW);
		clock = new Clock() {
			public ZoneId getZone() { return ZoneOffset.UTC; }
			public Clock withZone(ZoneId zone) { return this; }
			public Instant instant() { return time.get(); }
		};
		sessions = mock(RefreshSessionRepository.class); responses = mock(RefreshReissueResponseRepository.class);
		when(sessions.findByTokenHash(anyString())).thenAnswer(i -> db.values().stream()
				.filter(s -> s.getTokenHash().equals(i.getArgument(0))).findFirst().map(this::copySession));
		when(sessions.findById(anyString())).thenAnswer(i -> Optional.ofNullable(db.get(i.getArgument(0))).map(this::copySession));
		when(sessions.save(any())).thenAnswer(i -> save(i.getArgument(0)));
		when(sessions.saveAll(any())).thenAnswer(i -> {
			List<RefreshSession> saved = new ArrayList<>();
			Iterable<RefreshSession> values = i.getArgument(0);
			for (RefreshSession session : values) saved.add(save(session));
			return saved;
		});
		when(sessions.findAllByUserIdAndRevokedAtIsNull(anyString())).thenAnswer(i -> db.values().stream()
				.filter(s -> s.getUserId().equals(i.getArgument(0)) && !s.isRevoked()).map(this::copySession).toList());
		when(sessions.existsByUserIdAndRotationRequestKeyHash(anyString(), anyString())).thenAnswer(i -> db.values().stream()
				.anyMatch(s -> s.getUserId().equals(i.getArgument(0)) && Objects.equals(s.getRotationRequestKeyHash(), i.getArgument(1))));
		when(responses.insert(any(RefreshReissueResponse.class))).thenAnswer(i -> {
			var doc = i.getArgument(0, RefreshReissueResponse.class); payloads.put(doc.getResponseId(), doc); return doc;
		});
		when(responses.findById(anyString())).thenAnswer(i -> Optional.ofNullable(payloads.get(i.getArgument(0))));
		users = mock(UserRepository.class); user = mock(User.class);
		when(user.getAccountType()).thenReturn(UserAccountType.GUEST);
		when(users.findById(USER)).thenReturn(Optional.of(user));
		security = mock(SessionSecurityService.class); control = new UserSessionControl(USER);
		when(security.control(USER)).thenReturn(control);
		doAnswer(i -> { var s = i.getArgument(0, RefreshSession.class); control.validate(s.getSessionEpoch(), s.getAuthentication(), false); return null; })
				.when(security).checkAndTouch(any(), anyBoolean());
		generator = mock(RefreshTokenGenerator.class);
		when(generator.generate()).thenAnswer(i -> "test-only-" + UUID.randomUUID());
		refreshIssuer = new RefreshSessionIssuer(generator, hasher, sessions, new RefreshTokenProperties(Duration.ofDays(14), 32), clock);
		refreshIssuer.setSessionSecurity(security);
		accessIssuer = mock(AccessTokenIssuer.class);
		when(accessIssuer.issue(anyString(), any(), anySet())).thenAnswer(i -> access(i.getArgument(0), i.getArgument(1)));
		properties = new ReissueRecoveryProperties(); properties.setEnabled(true); properties.setEnvironment("unit-test");
		ReissueEncryptionKeyProvider keys = new ReissueEncryptionKeyProvider() {
			public String activeKeyId() { return "test-v1"; }
			public SecretKey key(String id) { if (!id.equals("test-v1")) throw SessionSecurityService.unavailable(); return new SecretKeySpec(new byte[32], "AES"); }
		};
		cipher = spy(new AesGcmReissueResponseCipher(keys, new ObjectMapper().findAndRegisterModules(), "unit-test", 16384));
		tx = mock(TransactionTemplate.class);
		when(tx.execute(any())).thenAnswer(i -> transaction(i.getArgument(0)));
		metrics = new SimpleMeterRegistry();
		service = new ReissueRecoveryService(sessions, responses, users, hasher, refreshIssuer, accessIssuer,
				new AuthResponseConverter(), security, tx, cipher, properties, clock, metrics);
		var source = RefreshSession.create(USER, hasher.hash(RAW), NOW.minusSeconds(10), NOW.plusSeconds(3600));
		source.attachAuthentication(new SessionAuthentication(0, SessionAuthentication.Source.GUEST, null, null));
		sourceId = source.getSessionId(); save(source);
	}
	Object transaction(TransactionCallback<?> callback) {
		Map<String, RefreshSession> before = new HashMap<>(); db.forEach((k, v) -> before.put(k, copySession(v)));
		Map<String, RefreshReissueResponse> beforePayloads = new HashMap<>(payloads);
		try { return callback.doInTransaction(new SimpleTransactionStatus()); }
		catch (RuntimeException e) { db = before; payloads = beforePayloads; throw e; }
	}
	RefreshSession copySession(RefreshSession s) { return json.convertValue(s, RefreshSession.class); }
	RefreshSession save(RefreshSession s) {
		var stored = db.get(s.getSessionId());
		if (stored != null && !Objects.equals(stored.getVersion(), s.getVersion())) throw new OptimisticLockingFailureException("test conflict");
		ReflectionTestUtils.setField(s, "version", stored == null ? 0L : stored.getVersion() + 1);
		db.put(s.getSessionId(), copySession(s)); return s;
	}
	IssuedAccessToken access(String userId, UserAccountType type) throws Exception {
		Instant issued = time.get().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
		var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), new JWTClaimsSet.Builder()
				.subject(userId).claim("account_type", type.name()).jwtID(UUID.randomUUID().toString())
				.issueTime(Date.from(issued)).expirationTime(Date.from(issued.plusSeconds(1800))).build());
		// Only a parser fixture for encrypted consistency checks. Production signing remains RS256 and has separate regression tests.
		jwt.sign(new MACSigner(new byte[32]));
		return new IssuedAccessToken(jwt.serialize(), "Bearer", issued, issued.plusSeconds(1800), 1800);
	}
	ReissueResult issue() { return service.reissue(new ReissueRequest(RAW), List.of(ID)); }
	RefreshSession source() { return db.get(sourceId); }
	RefreshSession child() { return db.get(source().getReplacedBySessionId()); }
	void error(AuthErrorStatus status, org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(status));
	}

	@Test void firstAndReplayAreExactlySameWithoutNewIssuance() {
		var first = issue(); var storedUntil = source().getRecoveryUntil(); time.set(NOW.plusSeconds(45));
		assertThat(issue()).isEqualTo(first);
		assertThat(db).hasSize(2); assertThat(payloads).hasSize(1);
		assertThat(source().getRecoveryUntil()).isEqualTo(storedUntil);
		verify(generator, times(1)).generate(); verify(accessIssuer, times(1)).issue(USER, UserAccountType.GUEST, Set.of());
		assertThat(child().getAuthentication()).isEqualTo(source().getAuthentication());
		assertThat(payloads.values().iterator().next().toString()).doesNotContain(first.response().refreshToken());
	}
	@ParameterizedTest @EnumSource(UserAccountType.class)
	void usesCurrentDatabaseType(UserAccountType type) { when(user.getAccountType()).thenReturn(type); issue(); verify(accessIssuer).issue(USER, type, Set.of()); }
	@Test void missingAccountTypeNeverDefaultsToMember() { when(user.getAccountType()).thenReturn(null); error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); verifyNoInteractions(accessIssuer); }
	@ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"bad", "11111111-1111-4111-A111-111111111111", "11111111-1111-1111-8111-111111111111", "11111111-1111-4111-8111-111111111111,22222222-2222-4222-8222-222222222222"})
	void rejectsMalformedRequestIds(String id) { error(AuthErrorStatus.INVALID_REISSUE_REQUEST_ID, () -> service.reissue(new ReissueRequest(RAW), Collections.singletonList(id))); verifyNoInteractions(accessIssuer); }
	@Test void rejectsMissingAndDuplicateHeaders() {
		error(AuthErrorStatus.INVALID_REISSUE_REQUEST_ID, () -> service.reissue(new ReissueRequest(RAW), List.of()));
		error(AuthErrorStatus.INVALID_REISSUE_REQUEST_ID, () -> service.reissue(new ReissueRequest(RAW), List.of(ID, ID)));
	}
	@Test void requestIdAloneCannotRetrieveResponse() { issue(); error(AuthErrorStatus.INVALID_REFRESH_TOKEN, () -> service.reissue(new ReissueRequest("test-unknown"), List.of(ID))); }
	@Test void deadlineIsExclusiveAndPayloadDeletionDoesNotCauseGlobalRevocation() {
		issue(); time.set(NOW.plusSeconds(119)); issue(); time.set(NOW.plusSeconds(120)); payloads.clear();
		error(AuthErrorStatus.REISSUE_RECOVERY_EXPIRED, this::issue); assertThat(child().isRevoked()).isFalse();
		time.set(NOW.plusSeconds(121)); error(AuthErrorStatus.REISSUE_RECOVERY_EXPIRED, this::issue);
	}
	@Test void expiredSourceCannotRevokeNewerSessions() {
		issue(); time.set(NOW.plusSeconds(3600));
		error(AuthErrorStatus.REFRESH_TOKEN_EXPIRED, () -> service.reissue(new ReissueRequest(RAW), List.of(OTHER_ID)));
		assertThat(child().isRevoked()).isFalse();
	}
	@Test void sourceExpiryShortensRecoveryWindow() {
		ReflectionTestUtils.setField(source(), "expiresAt", NOW.plusSeconds(15)); var first = issue();
		assertThat(first.recoveryUntil()).isEqualTo(NOW.plusSeconds(15));
	}
	@Test void accessExpiryEarlierThanRecoveryDeadlineIsNotExtended() {
		doAnswer(i -> {
			var normal = access(USER, UserAccountType.GUEST);
			var claims = new JWTClaimsSet.Builder(SignedJWT.parse(normal.tokenValue()).getJWTClaimsSet())
					.expirationTime(Date.from(NOW.plusSeconds(1))).build();
			var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims); jwt.sign(new MACSigner(new byte[32]));
			return new IssuedAccessToken(jwt.serialize(), "Bearer", NOW, NOW.plusSeconds(1), 1);
		}).when(accessIssuer).issue(anyString(), any(), anySet());
		assertThat(issue().accessExpiresAt()).isEqualTo(NOW.plusSeconds(1)); time.set(NOW.plusSeconds(1));
		error(AuthErrorStatus.REISSUE_RECOVERY_EXPIRED, this::issue); verify(generator, times(1)).generate();
	}
	@Test void expiredChildCannotBeReplayed() {
		issue(); ReflectionTestUtils.setField(child(), "expiresAt", NOW);
		error(AuthErrorStatus.SESSION_LOGGED_OUT, this::issue); verify(cipher, never()).decrypt(any(), any());
	}
	@Test void childOwnershipOrCredentialMismatchFailsClosed() {
		issue(); var original = copySession(child()); ReflectionTestUtils.setField(child(), "userId", UUID.randomUUID().toString());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue);
		db.put(original.getSessionId(), original); ReflectionTestUtils.setField(child(), "tokenHash", "test-mismatched-hash");
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); verify(generator, times(1)).generate();
	}
	@Test void sameUserRequestIdCannotBeReusedForDifferentSource() {
		var first = issue(); error(AuthErrorStatus.REISSUE_REQUEST_CONFLICT,
				() -> service.reissue(new ReissueRequest(first.response().refreshToken()), List.of(ID)));
		assertThat(child().isRevoked()).isFalse();
	}
	@Test void changedRequestIdRevokesAllActiveSessionsAndCommitsDespiteError() {
		issue(); var another = RefreshSession.create(USER, hasher.hash("test-other-login"), NOW, NOW.plusSeconds(3600)); save(another);
		error(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED, () -> service.reissue(new ReissueRequest(RAW), List.of(OTHER_ID)));
		assertThat(child().getRevocationReason()).isEqualTo(RevocationReason.REUSE_DETECTED);
		assertThat(db.get(another.getSessionId()).getRevocationReason()).isEqualTo(RevocationReason.REUSE_DETECTED);
	}
	@Test void legacyRotatedSourceStillUsesExistingReusePolicy() {
		var s = copySession(source()); s.rotate(NOW, UUID.randomUUID().toString()); save(s);
		error(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED, this::issue);
	}
	@Test void subsequentChildRotationSupersedesOldResult() {
		var first = issue(); service.reissue(new ReissueRequest(first.response().refreshToken()), List.of(OTHER_ID));
		error(AuthErrorStatus.REISSUE_RESULT_SUPERSEDED, this::issue); assertThat(db).hasSize(3);
	}
	@Test void changedAccountTypeDoesNotReplayOldGuestResult() { issue(); when(user.getAccountType()).thenReturn(UserAccountType.MEMBER); error(AuthErrorStatus.REISSUE_RESULT_SUPERSEDED, this::issue); }
	@Test void childLogoutPreventsReplay() { var first = issue(); service.logout(first.response().refreshToken()); error(AuthErrorStatus.SESSION_LOGGED_OUT, this::issue); }
	@Test void sourceLogoutCancelsOnlyItsDirectChildAndIsIdempotent() {
		issue(); var other = RefreshSession.create(USER, hasher.hash("test-other"), NOW, NOW.plusSeconds(500)); save(other);
		service.logout(RAW); service.logout(RAW);
		assertThat(source().getRecoveryDisabledAt()).isEqualTo(NOW); assertThat(child().getRevocationReason()).isEqualTo(RevocationReason.LOGOUT);
		assertThat(db.get(other.getSessionId()).isRevoked()).isFalse(); error(AuthErrorStatus.SESSION_LOGGED_OUT, this::issue);
	}
	@Test void oldSourceLogoutNeverTraversesToGrandchild() {
		var first = issue(); var second = service.reissue(new ReissueRequest(first.response().refreshToken()), List.of(OTHER_ID));
		service.logout(RAW); assertThat(sessions.findByTokenHash(hasher.hash(second.response().refreshToken())).orElseThrow().isRevoked()).isFalse();
	}
	@Test void cancelledRecoveryCannotPunishOtherSessionsWithDifferentRequestId() {
		issue(); service.logout(RAW);
		var other = RefreshSession.create(USER, hasher.hash("test-new-login"), NOW, NOW.plusSeconds(3600)); save(other);
		error(AuthErrorStatus.SESSION_LOGGED_OUT, () -> service.reissue(new ReissueRequest(RAW), List.of(OTHER_ID)));
		assertThat(db.get(other.getSessionId()).isRevoked()).isFalse();
	}
	@Test void logoutUnknownExpiredOrTerminalIsNoop() {
		service.logout("test-unknown"); issue(); time.set(NOW.plusSeconds(3600)); service.logout(RAW);
		assertThat(source().getRecoveryDisabledAt()).isNull();
	}
	@Test void epochFenceWinsBeforeReuseAndPreservesFreshSessions() {
		issue(); control.logout(null, NOW); error(AuthErrorStatus.SESSION_LOGGED_OUT,
				() -> service.reissue(new ReissueRequest(RAW), List.of(OTHER_ID))); assertThat(child().isRevoked()).isFalse();
	}
	@Test void withdrawnSourceReturnsDedicatedErrorBeforeReplay() {
		var s = copySession(source()); s.withdrawAccount(NOW); save(s); error(AuthErrorStatus.ACCOUNT_WITHDRAWN, this::issue);
	}
	@Test void accountWithdrawalDuringRecoveryWins() {
		issue(); doThrow(new AuthException(AuthErrorStatus.ACCOUNT_WITHDRAWN)).when(security).checkAndTouch(any(), eq(false));
		error(AuthErrorStatus.ACCOUNT_WITHDRAWN, this::issue); verify(cipher, never()).decrypt(any(), any());
	}
	@Test void confirmedFirebaseBoundaryIsAppliedOnReplay() {
		var s = copySession(source()); ReflectionTestUtils.setField(s, "authentication", new SessionAuthentication(0, SessionAuthentication.Source.FIREBASE, "test-binding", NOW.minusSeconds(60))); save(s);
		issue(); control.confirmRevocation("test-binding", NOW); error(AuthErrorStatus.SESSION_LOGGED_OUT, this::issue);
	}
	@Test void missingPayloadBeforeDeadlineIsUnavailableNotNewIssuance() { issue(); payloads.clear(); error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); verify(generator, times(1)).generate(); }
	@Test void encryptionFailureRollsBackSourceAndChild() {
		doThrow(SessionSecurityService.unavailable()).when(cipher).encrypt(any(), any());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); assertThat(db).hasSize(1); assertThat(source().isRevoked()).isFalse(); assertThat(payloads).isEmpty();
	}
	@Test void responseInsertFailureRollsBackEverything() {
		when(responses.insert(any(RefreshReissueResponse.class))).thenThrow(new DataAccessResourceFailureException("test failure"));
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); assertThat(db).hasSize(1); assertThat(source().isRevoked()).isFalse();
	}
	@Test void childSaveFailureRollsBackSource() {
		doAnswer(i -> { var s = i.getArgument(0, RefreshSession.class); if (!s.getSessionId().equals(sourceId)) throw new DataAccessResourceFailureException("test failure"); return save(s); }).when(sessions).save(any());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); assertThat(source().isRevoked()).isFalse();
	}
	@Test void failedReuseRevocationCannotReportSuccessfulPunishment() {
		issue(); doThrow(new DataAccessResourceFailureException("test failure")).when(sessions).saveAll(any());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, () -> service.reissue(new ReissueRequest(RAW), List.of(OTHER_ID)));
		assertThat(child().isRevoked()).isFalse();
	}
	@Test void failedChildLogoutRollsBackSourceCancellation() {
		issue(); doAnswer(i -> {
			var s = i.getArgument(0, RefreshSession.class);
			if (!s.getSessionId().equals(sourceId)) throw new DataAccessResourceFailureException("test failure");
			return save(s);
		}).when(sessions).save(any());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, () -> service.logout(RAW));
		assertThat(source().getRecoveryDisabledAt()).isNull(); assertThat(child().isRevoked()).isFalse();
	}
	@Test void sourceSaveFailureDoesNotIssue() {
		doThrow(new DataAccessResourceFailureException("test failure")).when(sessions).save(any()); error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); verifyNoInteractions(accessIssuer);
	}
	@Test void unknownCommitAfterSuccessOnlyReplaysPersistedResult() {
		doAnswer(i -> { transaction(i.getArgument(0)); throw unknown(); })
				.doAnswer(i -> transaction(i.getArgument(0))).when(tx).execute(any());
		assertThat(issue().response()).isNotNull(); verify(generator, times(1)).generate(); assertThat(db).hasSize(2);
	}
	@Test void unknownCommitWithoutReceiptNeverMints() {
		doThrow(unknown()).doAnswer(i -> transaction(i.getArgument(0))).when(tx).execute(any());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); verifyNoInteractions(generator);
	}
	@Test void boundedConflictRetriesAndNoRawExceptionLeak() {
		doThrow(new OptimisticLockingFailureException("test sensitive exception")).when(tx).execute(any());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); verify(tx, times(3)).execute(any());
	}
	@Test void conflictRereadsWinnerWithoutAnotherChild() {
		var first = issue(); doThrow(new OptimisticLockingFailureException("test conflict"))
				.doAnswer(i -> transaction(i.getArgument(0))).when(tx).execute(any());
		assertThat(issue()).isEqualTo(first); verify(generator, times(1)).generate();
	}
	@Test void lateCommitDoesNotExtendDeadline() {
		doAnswer(i -> { Object result = transaction(i.getArgument(0)); time.set(NOW.plusSeconds(121)); return result; }).when(tx).execute(any());
		error(AuthErrorStatus.REISSUE_RECOVERY_EXPIRED, this::issue); assertThat(child().isRevoked()).isFalse(); assertThat(source().getRecoveryUntil()).isEqualTo(NOW.plusSeconds(120));
	}
	@Test void maintenanceDoesNotIssueButAllowsLogout() { issue(); properties.setMaintenance(true); error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, this::issue); service.logout(RAW); assertThat(child().isRevoked()).isTrue(); }
	@Test void nanosecondClockDoesNotBreakPersistedReplay() { time.set(NOW.plusNanos(123456789)); var first = issue(); assertThat(issue()).isEqualTo(first); }
	@Test void turningFeatureOffCannotClassifyKnownRecoveryAsAttack() {
		issue(); var legacy = new TokenReissueService(hasher, sessions, users, accessIssuer, refreshIssuer, new AuthResponseConverter(), clock);
		when(security.transaction(any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
		error(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, () -> legacy.reissue(new ReissueRequest(RAW))); assertThat(child().isRevoked()).isFalse();
	}
	@Test void enabledServiceCannotBeCalledWithoutRequestIdThroughLegacyOverload() {
		var entry = new TokenReissueService(hasher, sessions, users, accessIssuer, refreshIssuer, new AuthResponseConverter(), clock);
		entry.setRecovery(service);
		error(AuthErrorStatus.INVALID_REISSUE_REQUEST_ID, () -> entry.reissue(new ReissueRequest(RAW)));
		assertThat(entry.reissue(new ReissueRequest(RAW), List.of(ID)).response()).isNotNull();
	}
	static MongoException unknown() { var e = new MongoException("test unknown commit"); e.addLabel("UnknownTransactionCommitResult"); return e; }
}
