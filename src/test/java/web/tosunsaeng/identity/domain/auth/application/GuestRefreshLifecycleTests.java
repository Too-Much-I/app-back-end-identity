package web.tosunsaeng.identity.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.dto.request.LogoutRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.AudioConsent;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenGenerator;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenProperties;

class GuestRefreshLifecycleTests {

	private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);
	private static final String CURRENT_REFRESH_VALUE = "guest-current-refresh-test-value";
	private static final String NEXT_REFRESH_VALUE = "guest-next-refresh-test-value";

	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();

	@Test
	void guestRefreshTokenUsesSharedReissueAndRotationFlow() {
		User guest = guest("A".repeat(43));
		RefreshSession currentSession = activeSession(
				guest.getUserId(),
				CURRENT_REFRESH_VALUE
		);
		RefreshSessionRepository sessionRepository = sessionRepository();
		UserRepository userRepository = mock(UserRepository.class);
		AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
		RefreshTokenGenerator tokenGenerator = mock(RefreshTokenGenerator.class);
		when(sessionRepository.findByTokenHash(refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)))
				.thenReturn(Optional.of(currentSession));
		when(userRepository.findById(guest.getUserId())).thenReturn(Optional.of(guest));
		when(accessTokenIssuer.issue(guest.getUserId(), Set.of()))
				.thenReturn(issuedAccessToken());
		when(tokenGenerator.generate()).thenReturn(NEXT_REFRESH_VALUE);

		TokenReissueService service = tokenReissueService(
				sessionRepository,
				userRepository,
				accessTokenIssuer,
				tokenGenerator
		);
		ReissueResponse response = service.reissue(
				new ReissueRequest(CURRENT_REFRESH_VALUE)
		);

		ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
		verify(sessionRepository, times(2)).save(captor.capture());
		RefreshSession replacement = captor.getAllValues().get(1);
		assertThat(guest.getProvider()).isEqualTo(UserProvider.GUEST);
		assertThat(currentSession.getRevocationReason()).isEqualTo(RevocationReason.ROTATED);
		assertThat(replacement.getUserId()).isEqualTo(guest.getUserId());
		assertThat(replacement.getRotatedFromSessionId())
				.isEqualTo(currentSession.getSessionId());
		assertThat(replacement.getTokenHash())
				.isEqualTo(refreshTokenHasher.hash(NEXT_REFRESH_VALUE))
				.isNotEqualTo(NEXT_REFRESH_VALUE);
		assertThat(response.accessToken()).isEqualTo("guest-next-access-test-value");
		assertThat(response.refreshToken()).isEqualTo(NEXT_REFRESH_VALUE);
		assertThat(response.accessTokenExpiresIn()).isEqualTo(ACCESS_TTL.toMillis());
		assertThat(response.refreshTokenExpiresIn()).isEqualTo(REFRESH_TTL.toMillis());
	}

	@Test
	void reusedRotatedGuestTokenRevokesActiveGuestSessions() {
		User guest = guest("A".repeat(43));
		RefreshSession rotatedSession = activeSession(
				guest.getUserId(),
				CURRENT_REFRESH_VALUE
		);
		String successorId = RefreshSession.newSessionId();
		rotatedSession.rotate(NOW.minusSeconds(1), successorId);
		RefreshSession activeSuccessor = RefreshSession.createRotated(
				successorId,
				guest.getUserId(),
				rotatedSession.getRotationFamilyId(),
				rotatedSession.getSessionId(),
				"guest-active-successor-test-hash",
				NOW.minusSeconds(1),
				NOW.plus(REFRESH_TTL)
		);
		RefreshSessionRepository sessionRepository = sessionRepository();
		when(sessionRepository.findByTokenHash(refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)))
				.thenReturn(Optional.of(rotatedSession));
		when(sessionRepository.findAllByUserIdAndRevokedAtIsNull(guest.getUserId()))
				.thenReturn(List.of(activeSuccessor));

		AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
		RefreshTokenGenerator tokenGenerator = mock(RefreshTokenGenerator.class);
		TokenReissueService service = tokenReissueService(
				sessionRepository,
				mock(UserRepository.class),
				accessTokenIssuer,
				tokenGenerator
		);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.reissue(new ReissueRequest(CURRENT_REFRESH_VALUE))
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED);
		assertThat(activeSuccessor.getRevocationReason())
				.isEqualTo(RevocationReason.REUSE_DETECTED);
		verify(sessionRepository).saveAll(List.of(activeSuccessor));
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(tokenGenerator, never()).generate();
	}

	@Test
	void guestSingleLogoutIsIdempotentThroughSharedSessionFlow() {
		User guest = guest("A".repeat(43));
		RefreshSession session = activeSession(guest.getUserId(), CURRENT_REFRESH_VALUE);
		RefreshSessionRepository repository = sessionRepository();
		when(repository.findByTokenHash(refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)))
				.thenReturn(Optional.of(session));
		LogoutService service = new LogoutService(refreshTokenHasher, repository, clock);

		service.logout(new LogoutRequest(CURRENT_REFRESH_VALUE));
		service.logout(new LogoutRequest(CURRENT_REFRESH_VALUE));

		assertThat(session.getUserId()).isEqualTo(guest.getUserId());
		assertThat(session.getRevocationReason()).isEqualTo(RevocationReason.LOGOUT);
		verify(repository, times(2)).findByTokenHash(
				refreshTokenHasher.hash(CURRENT_REFRESH_VALUE)
		);
		verify(repository, times(1)).save(session);
	}

	@Test
	void guestLogoutAllUsesJwtSubjectWithoutTouchingAnotherGuest() {
		User guest = guest("A".repeat(43));
		User otherGuest = guest("B".repeat(43));
		RefreshSession currentGuestSession = activeSession(
				guest.getUserId(),
				CURRENT_REFRESH_VALUE
		);
		RefreshSession otherGuestSession = activeSession(
				otherGuest.getUserId(),
				"other-guest-refresh-test-value"
		);
		CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
		RefreshSessionRepository repository = sessionRepository();
		when(currentUserProvider.getCurrentUserId()).thenReturn(guest.getUserId());
		when(repository.findAllByUserIdAndRevokedAtIsNull(guest.getUserId()))
				.thenReturn(List.of(currentGuestSession));
		LogoutAllService service = new LogoutAllService(currentUserProvider, repository, clock);

		service.logoutAll();

		assertThat(currentGuestSession.getRevocationReason())
				.isEqualTo(RevocationReason.LOGOUT_ALL);
		assertThat(otherGuestSession.isRevoked()).isFalse();
		verify(repository).findAllByUserIdAndRevokedAtIsNull(guest.getUserId());
		verify(repository, never()).findAllByUserIdAndRevokedAtIsNull(otherGuest.getUserId());
		verify(repository).saveAll(List.of(currentGuestSession));
	}

	private TokenReissueService tokenReissueService(
			RefreshSessionRepository sessionRepository,
			UserRepository userRepository,
			AccessTokenIssuer accessTokenIssuer,
			RefreshTokenGenerator tokenGenerator
	) {
		RefreshSessionIssuer sessionIssuer = new RefreshSessionIssuer(
				tokenGenerator,
				refreshTokenHasher,
				sessionRepository,
				new RefreshTokenProperties(REFRESH_TTL, 32),
				clock
		);
		return new TokenReissueService(
				refreshTokenHasher,
				sessionRepository,
				userRepository,
				accessTokenIssuer,
				sessionIssuer,
				new AuthResponseConverter(),
				clock
		);
	}

	private RefreshSessionRepository sessionRepository() {
		RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
		when(repository.save(any(RefreshSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(repository.saveAll(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
		return repository;
	}

	private RefreshSession activeSession(String userId, String tokenValue) {
		return RefreshSession.create(
				userId,
				refreshTokenHasher.hash(tokenValue),
				NOW.minusSeconds(60),
				NOW.plus(REFRESH_TTL)
		);
	}

	private User guest(String installationHash) {
		return User.createGuest(
				installationHash,
				UserFactory.GUEST_NICKNAME,
				AudioConsent.agreed("guest-test-audio-policy-v1", NOW.minusSeconds(60)),
				NOW.minusSeconds(60)
		);
	}

	private IssuedAccessToken issuedAccessToken() {
		return new IssuedAccessToken(
				"guest-next-access-test-value",
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				NOW,
				NOW.plus(ACCESS_TTL),
				ACCESS_TTL.toSeconds()
		);
	}
}
