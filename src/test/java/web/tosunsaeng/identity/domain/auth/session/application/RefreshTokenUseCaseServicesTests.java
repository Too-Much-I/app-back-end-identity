package web.tosunsaeng.identity.domain.auth.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.session.dto.request.LogoutRequest;
import web.tosunsaeng.identity.domain.auth.session.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenGenerator;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenProperties;
import web.tosunsaeng.identity.support.LogCapture;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class RefreshTokenUseCaseServicesTests {

	private static final Instant NOW = Instant.parse("2026-07-27T03:04:05Z");
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String CURRENT_REFRESH_VALUE = "current-test-refresh-value";
	private static final String NEXT_REFRESH_VALUE = "next-test-refresh-value";

	private UserRepository userRepository;
	private AccessTokenIssuer accessTokenIssuer;
	private RefreshSessionRepository refreshSessionRepository;
	private RefreshTokenGenerator refreshTokenGenerator;
	private RefreshTokenHasher refreshTokenHasher;
	private RefreshSessionIssuer refreshSessionIssuer;
	private TokenReissueService tokenReissueService;
	private LogoutService logoutService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		accessTokenIssuer = mock(AccessTokenIssuer.class);
		refreshSessionRepository = mock(RefreshSessionRepository.class);
		refreshTokenGenerator = mock(RefreshTokenGenerator.class);
		refreshTokenHasher = new RefreshTokenHasher();
		RefreshTokenProperties properties = new RefreshTokenProperties(
				REFRESH_TOKEN_TTL,
				32
		);
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		refreshSessionIssuer = new RefreshSessionIssuer(
				refreshTokenGenerator,
				refreshTokenHasher,
				refreshSessionRepository,
				properties,
				clock
		);
		when(refreshSessionRepository.save(any(RefreshSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AuthResponseConverter authResponseConverter = new AuthResponseConverter();
		tokenReissueService = new TokenReissueService(
				refreshTokenHasher,
				refreshSessionRepository,
				userRepository,
				accessTokenIssuer,
				refreshSessionIssuer,
				authResponseConverter,
				clock
		);
		logoutService = new LogoutService(
				refreshTokenHasher,
				refreshSessionRepository,
				clock
		);
	}

	@Test
	void reissuesBothTokensAfterHashLookupAndPersistsLinkedRotation() {
		RefreshSession currentSession = activeSession(CURRENT_REFRESH_VALUE);
		User user = user(UserStatus.ACTIVE);
		when(refreshSessionRepository.findByTokenHash(
				refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)
		)).thenReturn(Optional.of(currentSession));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		IssuedAccessToken accessToken = issuedAccessToken();
		when(accessTokenIssuer.issue(USER_ID, Set.of())).thenReturn(accessToken);
		when(refreshTokenGenerator.generate()).thenReturn(NEXT_REFRESH_VALUE);

		ReissueResponse response;
		try (LogCapture logs = LogCapture.forClass(TokenReissueService.class)) {
			response = tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE));
			assertThat(logs.events("auth.refresh.reissued")).singleElement()
					.satisfies(event -> {
						assertThat(event.getFormattedMessage())
								.isEqualTo("Refresh Token을 재발급했습니다");
						assertThat(LogCapture.value(event, "outcome")).isEqualTo("rotated");
						assertThat(LogCapture.value(event, "userId")).isEqualTo(USER_ID);
						assertThat(LogCapture.rendered(event)).doesNotContain(
								CURRENT_REFRESH_VALUE,
								NEXT_REFRESH_VALUE,
								refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)
						);
					});
		}

		verify(refreshSessionRepository).findByTokenHash(
				refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)
		);
		verify(userRepository).findById(USER_ID);
		verify(accessTokenIssuer).issue(USER_ID, Set.of());
		verify(refreshTokenGenerator).generate();

		ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
		verify(refreshSessionRepository, times(2)).save(sessionCaptor.capture());
		RefreshSession rotatedSession = sessionCaptor.getAllValues().get(0);
		RefreshSession nextSession = sessionCaptor.getAllValues().get(1);

		assertThat(rotatedSession).isSameAs(currentSession);
		assertThat(rotatedSession.getRevokedAt()).isEqualTo(NOW);
		assertThat(rotatedSession.getLastUsedAt()).isEqualTo(NOW);
		assertThat(rotatedSession.getRevocationReason()).isEqualTo(RevocationReason.ROTATED);
		assertThat(rotatedSession.getReplacedBySessionId()).isEqualTo(nextSession.getSessionId());
		assertThat(nextSession.getUserId()).isEqualTo(USER_ID);
		assertThat(nextSession.getRotatedFromSessionId()).isEqualTo(currentSession.getSessionId());
		assertThat(nextSession.getRotationFamilyId())
				.isEqualTo(currentSession.getRotationFamilyId());
		assertThat(nextSession.getCreatedAt()).isEqualTo(NOW);
		assertThat(nextSession.getLastUsedAt()).isEqualTo(NOW);
		assertThat(nextSession.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TOKEN_TTL));
		assertThat(nextSession.getRevokedAt()).isNull();
		assertThat(nextSession.getReplacedBySessionId()).isNull();
		assertThat(nextSession.getRevocationReason()).isNull();
		assertThat(nextSession.getTokenHash())
				.isEqualTo(refreshTokenHasher.hash(NEXT_REFRESH_VALUE))
				.isNotEqualTo(NEXT_REFRESH_VALUE);

		assertThat(response.accessToken()).isEqualTo(accessToken.tokenValue());
		assertThat(response.refreshToken())
				.isEqualTo(NEXT_REFRESH_VALUE)
				.isNotEqualTo(CURRENT_REFRESH_VALUE);
		assertThat(response.grantType()).isEqualTo("Bearer");
		assertThat(response.accessTokenExpiresIn()).isEqualTo(1_800_000L);
		assertThat(response.refreshTokenExpiresIn()).isEqualTo(1_209_600_000L);
		assertThat(response.toString())
				.contains("accessToken=redacted", "refreshToken=redacted")
				.doesNotContain(response.accessToken(), response.refreshToken());
	}

	@Test
	void rejectsUnknownRefreshTokenWithoutIssuingOrPersistingAnything() {
		when(refreshSessionRepository.findByTokenHash(any())).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN);
		assertThat(exception.getMessage()).doesNotContain(CURRENT_REFRESH_VALUE);
		verifyNoNewTokensOrSessions();
	}

	@Test
	void rejectsRefreshTokenAtItsExactExpirationBoundary() {
		RefreshSession expiredSession = RefreshSession.create(
				USER_ID,
				refreshTokenHasher.hash(CURRENT_REFRESH_VALUE),
				NOW.minus(REFRESH_TOKEN_TTL),
				NOW
		);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(expiredSession));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.REFRESH_TOKEN_EXPIRED);
		verify(userRepository, never()).findById(any());
		verifyNoNewTokensOrSessions();
	}

	@Test
	void rejectsLoggedOutRefreshTokenWithoutTreatingItAsReuse() {
		RefreshSession loggedOutSession = activeSession(CURRENT_REFRESH_VALUE);
		loggedOutSession.logout(NOW.minusSeconds(1));
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(loggedOutSession));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN);
		verify(refreshSessionRepository, never())
				.findAllByUserIdAndRevokedAtIsNull(any());
		verifyNoNewTokensOrSessions();
	}

	@Test
	void detectsRotatedTokenReuseAndRevokesEveryActiveUserSession() {
		RefreshSession rotatedSession = activeSession(CURRENT_REFRESH_VALUE);
		String successorId = RefreshSession.newSessionId();
		rotatedSession.rotate(NOW.minusSeconds(1), successorId);
		RefreshSession firstActiveSession = RefreshSession.createRotated(
				successorId,
				USER_ID,
				rotatedSession.getRotationFamilyId(),
				rotatedSession.getSessionId(),
				"first-active-test-hash",
				NOW.minusSeconds(1),
				NOW.plus(REFRESH_TOKEN_TTL)
		);
		RefreshSession secondActiveSession = RefreshSession.create(
				USER_ID,
				"second-active-test-hash",
				NOW.minusSeconds(1),
				NOW.plus(REFRESH_TOKEN_TTL)
		);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(rotatedSession));
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenReturn(List.of(firstActiveSession, secondActiveSession));
		when(refreshSessionRepository.saveAll(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		BusinessException exception;
		try (LogCapture logs = LogCapture.forClass(TokenReissueService.class)) {
			exception = catchThrowableOfType(
					BusinessException.class,
					() -> tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
			);
			assertThat(logs.events("auth.refresh.reuse_detected")).singleElement()
					.satisfies(event -> {
						assertThat(event.getFormattedMessage())
								.isEqualTo("Refresh Token 재사용을 감지했습니다");
						assertThat(LogCapture.value(event, "revokedSessionCount")).isEqualTo(2);
						assertThat(LogCapture.value(event, "errorCode"))
								.isEqualTo("REFRESH_TOKEN_REUSE_DETECTED");
						assertThat(LogCapture.rendered(event)).doesNotContain(
								CURRENT_REFRESH_VALUE,
								refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)
						);
					});
		}

		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED);
		assertThat(firstActiveSession.getRevokedAt()).isEqualTo(NOW);
		assertThat(firstActiveSession.getLastUsedAt()).isEqualTo(NOW);
		assertThat(firstActiveSession.getRevocationReason())
				.isEqualTo(RevocationReason.REUSE_DETECTED);
		assertThat(secondActiveSession.getRevokedAt()).isEqualTo(NOW);
		assertThat(secondActiveSession.getLastUsedAt()).isEqualTo(NOW);
		assertThat(secondActiveSession.getRevocationReason())
				.isEqualTo(RevocationReason.REUSE_DETECTED);
		verify(refreshSessionRepository).saveAll(List.of(firstActiveSession, secondActiveSession));
		verifyNoNewTokensOrSessions();
	}

	@ParameterizedTest
	@EnumSource(value = UserStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
	void rejectsNonActiveUserWithoutRotatingOrIssuingTokens(UserStatus status) {
		RefreshSession session = activeSession(CURRENT_REFRESH_VALUE);
		User inactiveUser = user(status);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(session));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(inactiveUser));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		assertThat(session.isRevoked()).isFalse();
		verifyNoNewTokensOrSessions();
	}

	@Test
	void treatsMissingSessionUserAsInvalidWithoutRotating() {
		RefreshSession session = activeSession(CURRENT_REFRESH_VALUE);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(session));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN);
		verifyNoNewTokensOrSessions();
	}

	@Test
	void optimisticLockConflictPreventsSecondRotationAndAllTokenIssuance() {
		RefreshSession session = activeSession(CURRENT_REFRESH_VALUE);
		User activeUser = user(UserStatus.ACTIVE);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(session));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser));
		when(refreshSessionRepository.save(session)).thenThrow(
				new OptimisticLockingFailureException("test-only version conflict")
		);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> tokenReissueService.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN);
		assertThat(exception.getMessage()).doesNotContain("version conflict");
		verify(refreshSessionRepository).save(session);
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshTokenGenerator, never()).generate();
	}

	@Test
	void logoutRevokesActiveSessionWithLogoutReasonAndNoTokenIssuance() {
		RefreshSession session = activeSession(CURRENT_REFRESH_VALUE);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(session));

		try (LogCapture logs = LogCapture.forClass(LogoutService.class)) {
			logoutService.logout(new LogoutRequest(CURRENT_REFRESH_VALUE));
			assertThat(logs.events("auth.logout.completed")).singleElement()
					.satisfies(event -> {
						assertThat(event.getFormattedMessage())
								.isEqualTo("로그아웃이 완료되었습니다");
						assertThat(LogCapture.value(event, "outcome"))
								.isEqualTo("session_revoked");
						assertThat(LogCapture.value(event, "revokedSessionCount")).isEqualTo(1);
						assertThat(LogCapture.rendered(event)).doesNotContain(
								CURRENT_REFRESH_VALUE,
								refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)
						);
					});
		}

		verify(refreshSessionRepository).findByTokenHash(
				refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)
		);
		verify(refreshSessionRepository).save(session);
		assertThat(session.getRevokedAt()).isEqualTo(NOW);
		assertThat(session.getLastUsedAt()).isEqualTo(NOW);
		assertThat(session.getRevocationReason()).isEqualTo(RevocationReason.LOGOUT);
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshTokenGenerator, never()).generate();
	}

	@Test
	void logoutSucceedsWithoutSaveForUnknownRevokedAndExpiredTokens() {
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.empty());
		logoutService.logout(new LogoutRequest("unknown-test-refresh-value"));

		RefreshSession revokedSession = activeSession("revoked-test-refresh-value");
		revokedSession.logout(NOW.minusSeconds(1));
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(revokedSession));
		logoutService.logout(new LogoutRequest("revoked-test-refresh-value"));

		RefreshSession expiredSession = RefreshSession.create(
				USER_ID,
				refreshTokenHasher.hash("expired-test-refresh-value"),
				NOW.minus(REFRESH_TOKEN_TTL),
				NOW
		);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(expiredSession));
		logoutService.logout(new LogoutRequest("expired-test-refresh-value"));

		verify(refreshSessionRepository, never()).save(any(RefreshSession.class));
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshTokenGenerator, never()).generate();
	}

	@Test
	void logoutDoesNotHideRepositoryFailureAsSuccess() {
		RefreshSession session = activeSession(CURRENT_REFRESH_VALUE);
		when(refreshSessionRepository.findByTokenHash(any()))
				.thenReturn(Optional.of(session));
		when(refreshSessionRepository.save(session)).thenThrow(
				new DataAccessResourceFailureException("test-only repository failure")
		);

		assertThatThrownBy(() -> logoutService.logout(new LogoutRequest(CURRENT_REFRESH_VALUE)))
				.isInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	void refreshRequestsAndErrorsRedactTokenValuesAndUseUnauthorizedStatus() {
		ReissueRequest reissueRequest = new ReissueRequest(CURRENT_REFRESH_VALUE);
		LogoutRequest logoutRequest = new LogoutRequest(CURRENT_REFRESH_VALUE);

		assertThat(reissueRequest.toString())
				.contains("refreshToken=redacted")
				.doesNotContain(CURRENT_REFRESH_VALUE);
		assertThat(logoutRequest.toString())
				.contains("refreshToken=redacted")
				.doesNotContain(CURRENT_REFRESH_VALUE);
		assertThat(List.of(
				AuthErrorStatus.INVALID_REFRESH_TOKEN,
				AuthErrorStatus.REFRESH_TOKEN_EXPIRED,
				AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED
		)).allSatisfy(error -> assertThat(error.getHttpStatus())
				.isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	private RefreshSession activeSession(String tokenValue) {
		return RefreshSession.create(
				USER_ID,
				refreshTokenHasher.hash(tokenValue),
				NOW.minusSeconds(60),
				NOW.plus(REFRESH_TOKEN_TTL)
		);
	}

	private User user(UserStatus status) {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(USER_ID);
		when(user.getStatus()).thenReturn(status);
		return user;
	}

	private IssuedAccessToken issuedAccessToken() {
		return new IssuedAccessToken(
				"next-test-access-value",
				"Bearer",
				NOW,
				NOW.plus(ACCESS_TOKEN_TTL),
				ACCESS_TOKEN_TTL.toSeconds()
		);
	}

	private void verifyNoNewTokensOrSessions() {
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshTokenGenerator, never()).generate();
		verify(refreshSessionRepository, never()).save(any(RefreshSession.class));
	}
}
