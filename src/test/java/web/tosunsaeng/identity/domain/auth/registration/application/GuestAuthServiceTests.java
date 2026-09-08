package web.tosunsaeng.identity.domain.auth.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.registration.dto.request.GuestAuthRequest;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.guest.GuestInstallationIdHasher;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.support.SignedUserTokenFixture;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.support.LogCapture;

class GuestAuthServiceTests {

	private static final String INSTALLATION_ID = "550e8400-e29b-41d4-a716-446655440000";
	private static final String OTHER_INSTALLATION_ID = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";
	private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);
	private static final String PRIVACY_VERSION = "privacy-v1";
	private static final String TERM_VERSION = "term-v1";
	private static final String QUALITY_REVIEW_VERSION = "quality-review-v1";

	private UserRepository userRepository;
	private AccessTokenIssuer accessTokenIssuer;
	private RefreshSessionIssuer refreshSessionIssuer;
	private GuestRegistrationTransactionService registrationTransactionService;
	private GuestInstallationIdHasher installationIdHasher;
	private ConsentPolicy consentPolicy;
	private UserFactory userFactory;
	private GuestAuthService guestAuthService;
	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		accessTokenIssuer = mock(AccessTokenIssuer.class);
		refreshSessionIssuer = mock(RefreshSessionIssuer.class);
		registrationTransactionService = mock(GuestRegistrationTransactionService.class);
		installationIdHasher = new GuestInstallationIdHasher();
		consentPolicy = new ConsentPolicy(
				PRIVACY_VERSION,
				TERM_VERSION,
				QUALITY_REVIEW_VERSION
		);
		userFactory = new UserFactory(
				new EmailNormalizer(),
				new BCryptPasswordEncoder(4),
				consentPolicy
		);
		guestAuthService = new GuestAuthService(
				userRepository,
				userFactory,
				consentPolicy,
				installationIdHasher,
				accessTokenIssuer,
				refreshSessionIssuer,
				registrationTransactionService,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		when(userRepository.existsByGuestInstallationIdHash(anyString())).thenReturn(false);
		when(accessTokenIssuer.issue(anyString(), any(), any())).thenAnswer(invocation ->
				issuedAccessToken()
		);
		when(refreshSessionIssuer.prepare(anyString())).thenAnswer(invocation ->
				preparedRefreshSession(invocation.getArgument(0))
		);
		when(registrationTransactionService.register(any(), any(), any())).thenAnswer(invocation ->
				response(invocation.getArgument(1), invocation.getArgument(2))
		);
		executor = Executors.newFixedThreadPool(2);
	}

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void preparesActiveGuestAndExistingTokensBeforeTransactionalRegistration() {
		SignedUserTokenFixture tokens = new SignedUserTokenFixture(NOW);
		tokens.delegate(accessTokenIssuer);
		GuestAuthResponse response;
		try (LogCapture logs = LogCapture.forClass(GuestAuthService.class)) {
			response = guestAuthService.authenticate(validRequest(INSTALLATION_ID));
			assertThat(logs.events("identity.guest.registered")).singleElement()
					.satisfies(event -> {
						assertThat(event.getFormattedMessage())
								.isEqualTo("게스트 사용자 등록이 완료되었습니다");
						assertThat(LogCapture.value(event, "accountType"))
								.isEqualTo(UserAccountType.GUEST);
						assertThat(LogCapture.value(event, "provider")).isNull();
						assertThat(LogCapture.rendered(event)).doesNotContain(
								INSTALLATION_ID,
								installationIdHasher.hash(INSTALLATION_ID),
								response.accessToken(),
								response.refreshToken()
						);
					});
		}

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<IssuedAccessToken> accessCaptor = ArgumentCaptor.forClass(
				IssuedAccessToken.class
		);
		ArgumentCaptor<PreparedRefreshSession> refreshCaptor = ArgumentCaptor.forClass(
				PreparedRefreshSession.class
		);
		verify(registrationTransactionService).register(
				userCaptor.capture(),
				accessCaptor.capture(),
				refreshCaptor.capture()
		);
		User guest = userCaptor.getValue();
		tokens.assertClaims(response.accessToken(), guest.getUserId(), UserAccountType.GUEST);
		PreparedRefreshSession preparedRefresh = refreshCaptor.getValue();

		assertThat(guest.getAccountType()).isEqualTo(UserAccountType.GUEST);
		assertThat(guest.getProvider()).isEqualTo(UserProvider.GUEST);
		assertThat(guest.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(guest.getEmail()).isNull();
		assertThat(guest.getNormalizedEmail()).isNull();
		assertThat(guest.getPasswordHash()).isNull();
		assertThat(guest.getNickname()).isEqualTo(UserFactory.GUEST_NICKNAME);
		assertThat(guest.getCreatedAt()).isEqualTo(NOW);
		assertThat(guest.getConsents().isPrivacyConsented()).isTrue();
		assertThat(guest.getConsents().getPrivacyConsentVersion()).isEqualTo(PRIVACY_VERSION);
		assertThat(guest.getConsents().getPrivacyConsentedAt()).isEqualTo(NOW);
		assertThat(guest.getConsents().isTermConsented()).isTrue();
		assertThat(guest.getConsents().getTermConsentVersion()).isEqualTo(TERM_VERSION);
		assertThat(guest.getConsents().getTermConsentedAt()).isEqualTo(NOW);
		assertThat(guest.getConsents().isQualityReviewConsented()).isFalse();
		assertThat(guest.getConsents().getQualityReviewConsentVersion()).isNull();
		assertThat(guest.getConsents().getQualityReviewConsentedAt()).isNull();
		assertThat(guest.getGuestInstallationIdHash())
				.isEqualTo(installationIdHasher.hash(INSTALLATION_ID))
				.hasSize(43)
				.doesNotContain(INSTALLATION_ID);
		assertThat(preparedRefresh.session().getUserId()).isEqualTo(guest.getUserId());
		assertThat(preparedRefresh.session().getTokenHash())
				.isNotEqualTo(preparedRefresh.tokenValue());
		verify(accessTokenIssuer).issue(guest.getUserId(), UserAccountType.GUEST, Set.of());
		verify(refreshSessionIssuer).prepare(guest.getUserId());
		assertThat(accessCaptor.getValue().tokenValue()).isEqualTo(response.accessToken());
		assertThat(response.refreshToken()).isEqualTo(preparedRefresh.tokenValue());
		assertThat(response.accessTokenExpiresIn()).isEqualTo(ACCESS_TTL.toMillis());
		assertThat(response.refreshTokenExpiresIn()).isEqualTo(REFRESH_TTL.toMillis());
	}

	@Test
	void storesOptionalQualityReviewConsentWhenCurrentVersionMatches() {
		guestAuthService.authenticate(new GuestAuthRequest(
				INSTALLATION_ID,
				true,
				PRIVACY_VERSION,
				true,
				TERM_VERSION,
				true,
				QUALITY_REVIEW_VERSION
		));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(registrationTransactionService).register(captor.capture(), any(), any());
		User guest = captor.getValue();
		assertThat(guest.getConsents().isQualityReviewConsented()).isTrue();
		assertThat(guest.getConsents().getQualityReviewConsentVersion())
				.isEqualTo(QUALITY_REVIEW_VERSION);
		assertThat(guest.getConsents().getQualityReviewConsentedAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsTrueQualityReviewConsentWithStaleVersionBeforeCreatingGuest() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(new GuestAuthRequest(
						INSTALLATION_ID,
						true,
						PRIVACY_VERSION,
						true,
						TERM_VERSION,
						true,
						"quality-review-v0"
				))
		);

		assertThat(exception.getErrorCode()).isEqualTo(
				UserErrorStatus.QUALITY_REVIEW_CONSENT_VERSION_MISMATCH
		);
		verify(userRepository, never()).existsByGuestInstallationIdHash(anyString());
		verify(registrationTransactionService, never()).register(any(), any(), any());
	}

	@Test
	void falseQualityReviewConsentIgnoresStaleVersion() {
		guestAuthService.authenticate(new GuestAuthRequest(
				INSTALLATION_ID,
				true,
				PRIVACY_VERSION,
				true,
				TERM_VERSION,
				false,
				"quality-review-v0"
		));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(registrationTransactionService).register(captor.capture(), any(), any());
		assertThat(captor.getValue().getConsents().isQualityReviewConsented()).isFalse();
		assertThat(captor.getValue().getConsents().getQualityReviewConsentVersion()).isNull();
	}

	@Test
	void differentInstallationIdsPrepareDifferentGuestUsers() {
		guestAuthService.authenticate(validRequest(INSTALLATION_ID));
		guestAuthService.authenticate(validRequest(OTHER_INSTALLATION_ID));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(registrationTransactionService, times(2)).register(captor.capture(), any(), any());
		List<User> guests = captor.getAllValues();
		assertThat(guests).extracting(User::getUserId).doesNotHaveDuplicates();
		assertThat(guests).extracting(User::getGuestInstallationIdHash).doesNotHaveDuplicates();
	}

	@Test
	void falsePrivacyConsentDoesNotPrepareOrPersistAnything() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(new GuestAuthRequest(
						INSTALLATION_ID,
						false,
						PRIVACY_VERSION,
						true,
						TERM_VERSION,
						false,
						null
				))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.PRIVACY_CONSENT_REQUIRED);
		verify(userRepository, never()).existsByGuestInstallationIdHash(anyString());
		verify(accessTokenIssuer, never()).issue(anyString(), any(), any());
		verify(refreshSessionIssuer, never()).prepare(anyString());
		verify(registrationTransactionService, never()).register(any(), any(), any());
	}

	@Test
	void falseTermConsentDoesNotPrepareOrPersistAnything() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(new GuestAuthRequest(
						INSTALLATION_ID,
						true,
						PRIVACY_VERSION,
						false,
						TERM_VERSION,
						false,
						null
				))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.TERM_CONSENT_REQUIRED);
		verify(userRepository, never()).existsByGuestInstallationIdHash(anyString());
		verify(accessTokenIssuer, never()).issue(anyString(), any(), any());
		verify(refreshSessionIssuer, never()).prepare(anyString());
		verify(registrationTransactionService, never()).register(any(), any(), any());
	}

	@Test
	void mismatchedConsentVersionDoesNotCreateGuest() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(new GuestAuthRequest(
						INSTALLATION_ID,
						true,
						"privacy-old",
						true,
						TERM_VERSION,
						false,
						null
				))
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(UserErrorStatus.PRIVACY_CONSENT_VERSION_MISMATCH);
		verify(userRepository, never()).existsByGuestInstallationIdHash(anyString());
		verify(registrationTransactionService, never()).register(any(), any(), any());
	}

	@Test
	void mismatchedTermVersionDoesNotCreateGuest() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(new GuestAuthRequest(
						INSTALLATION_ID,
						true,
						PRIVACY_VERSION,
						true,
						"term-old",
						false,
						null
				))
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(UserErrorStatus.TERM_CONSENT_VERSION_MISMATCH);
		verify(userRepository, never()).existsByGuestInstallationIdHash(anyString());
		verify(registrationTransactionService, never()).register(any(), any(), any());
	}

	@Test
	void existingInstallationReturnsConflictBeforePreparingNewTokens() {
		when(userRepository.existsByGuestInstallationIdHash(anyString())).thenReturn(true);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(validRequest(INSTALLATION_ID))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		verify(accessTokenIssuer, never()).issue(anyString(), any(), any());
		verify(refreshSessionIssuer, never()).prepare(anyString());
		verify(registrationTransactionService, never()).register(any(), any(), any());
	}

	@Test
	void accessTokenPreparationFailureLeavesNoPersistenceAndAllowsRetry() {
		when(accessTokenIssuer.issue(anyString(), any(), any()))
				.thenThrow(new IllegalStateException("test-only access preparation failure"))
				.thenReturn(issuedAccessToken());

		Throwable firstFailure = catchThrowable(
				() -> guestAuthService.authenticate(validRequest(INSTALLATION_ID))
		);
		assertThat(firstFailure).isInstanceOf(IllegalStateException.class);
		verify(refreshSessionIssuer, never()).prepare(anyString());
		verify(registrationTransactionService, never()).register(any(), any(), any());

		GuestAuthResponse retry = guestAuthService.authenticate(validRequest(INSTALLATION_ID));
		assertThat(retry.accessToken()).isNotBlank();
		verify(registrationTransactionService).register(any(), any(), any());
	}

	@Test
	void refreshTokenPreparationFailureLeavesNoPersistence() {
		doThrow(new IllegalStateException("test-only refresh preparation failure"))
				.when(refreshSessionIssuer).prepare(anyString());

		Throwable failure = catchThrowable(
				() -> guestAuthService.authenticate(validRequest(INSTALLATION_ID))
		);

		assertThat(failure).isInstanceOf(IllegalStateException.class);
		verify(registrationTransactionService, never()).register(any(), any(), any());
	}

	@Test
	void guestHashRaceDuplicateBecomesConflictOnlyAfterWinnerExists() {
		when(userRepository.existsByGuestInstallationIdHash(anyString()))
				.thenReturn(false, true);
		doThrow(new DuplicateKeyException("test-only duplicate"))
				.when(registrationTransactionService).register(any(), any(), any());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(validRequest(INSTALLATION_ID))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		assertThat(exception.getMessage()).doesNotContain(INSTALLATION_ID);
		verify(userRepository, times(2)).existsByGuestInstallationIdHash(anyString());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"test-only _id duplicate",
			"test-only normalizedEmail duplicate",
			"test-only unknown unique duplicate"
	})
	void nonGuestUniqueDuplicatesRemainOriginalPersistenceErrors(String internalDetail) {
		DuplicateKeyException duplicate = new DuplicateKeyException(internalDetail);
		when(userRepository.existsByGuestInstallationIdHash(anyString()))
				.thenReturn(false, false);
		doThrow(duplicate).when(registrationTransactionService).register(any(), any(), any());

		Throwable failure = catchThrowable(
				() -> guestAuthService.authenticate(validRequest(INSTALLATION_ID))
		);

		assertThat(failure).isSameAs(duplicate);
		assertThat(failure).isNotInstanceOf(BusinessException.class);
	}

	@Test
	void commitThenResponseLossRetryStaysConflictWithoutPreparingAnotherToken() {
		AtomicBoolean persisted = new AtomicBoolean(false);
		when(userRepository.existsByGuestInstallationIdHash(anyString())).thenAnswer(
				invocation -> persisted.get()
		);
		doAnswer(invocation -> {
			persisted.set(true);
			return response(invocation.getArgument(1), invocation.getArgument(2));
		}).when(registrationTransactionService).register(any(), any(), any());

		GuestAuthResponse committed = guestAuthService.authenticate(validRequest(INSTALLATION_ID));
		BusinessException retry = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(validRequest(INSTALLATION_ID))
		);

		assertThat(committed.refreshToken()).isNotBlank();
		assertThat(retry.getErrorCode()).isEqualTo(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		verify(accessTokenIssuer, times(1)).issue(anyString(), any(), any());
		verify(refreshSessionIssuer, times(1)).prepare(anyString());
		verify(registrationTransactionService, times(1)).register(any(), any(), any());
	}

	@Test
	void concurrentSameInstallationAllowsOneCommittedRegistrationAndOneConflict()
			throws Exception {
		ConcurrentHashMap<String, User> persisted = new ConcurrentHashMap<>();
		CountDownLatch initialChecks = new CountDownLatch(2);
		AtomicInteger existenceChecks = new AtomicInteger();
		when(userRepository.existsByGuestInstallationIdHash(anyString())).thenAnswer(invocation -> {
			int call = existenceChecks.incrementAndGet();
			if (call <= 2) {
				initialChecks.countDown();
				initialChecks.await();
				return false;
			}
			return persisted.containsKey(invocation.getArgument(0));
		});
		doAnswer(invocation -> {
			User candidate = invocation.getArgument(0);
			User previous = persisted.putIfAbsent(
					candidate.getGuestInstallationIdHash(),
					candidate
			);
			if (previous != null) {
				throw new DuplicateKeyException("test-only simulated guest hash conflict");
			}
			return response(invocation.getArgument(1), invocation.getArgument(2));
		}).when(registrationTransactionService).register(any(), any(), any());
		CountDownLatch start = new CountDownLatch(1);

		Future<Object> first = executor.submit(() -> authenticateAfter(start));
		Future<Object> second = executor.submit(() -> authenticateAfter(start));
		start.countDown();

		List<Object> results = List.of(first.get(), second.get());
		assertThat(results).filteredOn(GuestAuthResponse.class::isInstance).hasSize(1);
		assertThat(results).filteredOn(BusinessException.class::isInstance).hasSize(1);
		assertThat(persisted).hasSize(1);
		verify(registrationTransactionService, times(2)).register(any(), any(), any());
		verify(userRepository, atLeastOnce()).existsByGuestInstallationIdHash(anyString());
	}

	private Object authenticateAfter(CountDownLatch start) throws InterruptedException {
		start.await();
		try {
			return guestAuthService.authenticate(validRequest(INSTALLATION_ID));
		} catch (BusinessException exception) {
			return exception;
		}
	}

	private IssuedAccessToken issuedAccessToken() {
		return new IssuedAccessToken(
				"guest-access-test-value",
				"Bearer",
				NOW,
				NOW.plus(ACCESS_TTL),
				ACCESS_TTL.toSeconds()
		);
	}

	private PreparedRefreshSession preparedRefreshSession(String userId) {
		return new PreparedRefreshSession(
				"guest-refresh-test-value",
				RefreshSession.create(
						userId,
						"guest-refresh-test-hash",
						NOW,
						NOW.plus(REFRESH_TTL)
				)
		);
	}

	private GuestAuthResponse response(
			IssuedAccessToken accessToken,
			PreparedRefreshSession refreshSession
	) {
		return new AuthResponseConverter().toGuestAuthResponse(
				accessToken,
				refreshSession.tokenValue(),
				refreshSession.session().getCreatedAt(),
				refreshSession.session().getExpiresAt()
		);
	}

	private GuestAuthRequest validRequest(String installationId) {
		return new GuestAuthRequest(
				installationId,
				true,
				PRIVACY_VERSION,
				true,
				TERM_VERSION,
				false,
				QUALITY_REVIEW_VERSION
		);
	}
}
