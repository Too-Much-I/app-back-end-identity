package web.tosunsaeng.identity.domain.auth.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.support.LogCapture;

@SuppressWarnings("unchecked")
class LogoutAllServiceTests {

	private static final Instant NOW = Instant.parse("2026-07-27T04:05:06Z");
	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String OTHER_USER_ID = "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e";

	private CurrentUserProvider currentUserProvider;
	private RefreshSessionRepository refreshSessionRepository;
	private LogoutAllService logoutAllService;

	@BeforeEach
	void setUp() {
		currentUserProvider = mock(CurrentUserProvider.class);
		refreshSessionRepository = mock(RefreshSessionRepository.class);
		logoutAllService = new LogoutAllService(
				currentUserProvider,
				refreshSessionRepository,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
	}

	@Test
	void revokesEveryActiveSessionForJwtSubjectWithLogoutAllReasonAndSameClockTime() {
		RefreshSession first = activeSession(USER_ID, "first-test-hash");
		RefreshSession second = activeSession(USER_ID, "second-test-hash");
		List<RefreshSession> activeSessions = List.of(first, second);
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenReturn(activeSessions);

		try (LogCapture logs = LogCapture.forClass(LogoutAllService.class)) {
			logoutAllService.logoutAll();
			assertThat(logs.events("auth.logout_all.completed")).singleElement()
					.satisfies(event -> {
						assertThat(event.getFormattedMessage())
								.isEqualTo("전체 로그아웃이 완료되었습니다");
						assertThat(LogCapture.value(event, "outcome"))
								.isEqualTo("sessions_revoked");
						assertThat(LogCapture.value(event, "userId")).isEqualTo(USER_ID);
						assertThat(LogCapture.value(event, "revokedSessionCount")).isEqualTo(2);
						assertThat(LogCapture.rendered(event))
								.doesNotContain("first-test-hash", "second-test-hash");
					});
		}

		verify(currentUserProvider).getCurrentUserId();
		verify(refreshSessionRepository).findAllByUserIdAndRevokedAtIsNull(USER_ID);
		verify(refreshSessionRepository).saveAll(activeSessions);
		assertThat(activeSessions).allSatisfy(session -> {
			assertThat(session.getUserId()).isEqualTo(USER_ID);
			assertThat(session.getRevokedAt()).isEqualTo(NOW);
			assertThat(session.getLastUsedAt()).isEqualTo(NOW);
			assertThat(session.getRevocationReason()).isEqualTo(RevocationReason.LOGOUT_ALL);
		});
	}

	@Test
	void doesNotQueryOrModifyAnotherUsersSessions() {
		RefreshSession currentUserSession = activeSession(USER_ID, "current-user-test-hash");
		RefreshSession otherUserSession = activeSession(OTHER_USER_ID, "other-user-test-hash");
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenReturn(List.of(currentUserSession));

		logoutAllService.logoutAll();

		verify(refreshSessionRepository).findAllByUserIdAndRevokedAtIsNull(USER_ID);
		verify(refreshSessionRepository, never())
				.findAllByUserIdAndRevokedAtIsNull(OTHER_USER_ID);
		assertThat(currentUserSession.getRevocationReason()).isEqualTo(RevocationReason.LOGOUT_ALL);
		assertThat(otherUserSession.getRevokedAt()).isNull();
		assertThat(otherUserSession.getLastUsedAt()).isEqualTo(NOW.minusSeconds(60));
		assertThat(otherUserSession.getRevocationReason()).isNull();
	}

	@Test
	void succeedsWithoutSaveWhenNoActiveSessionsExist() {
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenReturn(List.of());

		logoutAllService.logoutAll();

		verify(refreshSessionRepository, never()).<RefreshSession>saveAll(anyList());
	}

	@Test
	void repeatedRequestSucceedsAndOnlyFirstRequestPersistsRevocation() {
		RefreshSession session = activeSession(USER_ID, "idempotent-test-hash");
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenReturn(List.of(session), List.of());

		logoutAllService.logoutAll();
		logoutAllService.logoutAll();

		verify(refreshSessionRepository, times(2))
				.findAllByUserIdAndRevokedAtIsNull(USER_ID);
		verify(refreshSessionRepository, times(1)).<RefreshSession>saveAll(anyList());
		assertThat(session.getRevocationReason()).isEqualTo(RevocationReason.LOGOUT_ALL);
	}

	@Test
	void doesNotHideRepositoryReadFailures() {
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenThrow(new DataAccessResourceFailureException("test repository failure"));

		assertThatThrownBy(logoutAllService::logoutAll)
				.isInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	void doesNotHideRepositorySaveFailures() {
		RefreshSession session = activeSession(USER_ID, "save-failure-test-hash");
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenReturn(List.of(session));
		doThrow(new DataAccessResourceFailureException("test save failure"))
				.when(refreshSessionRepository)
				.<RefreshSession>saveAll(anyList());

		assertThatThrownBy(logoutAllService::logoutAll)
				.isInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	void serviceHasNoTokenIssuerDependency() {
		boolean hasTokenIssuerDependency = Arrays.stream(
				LogoutAllService.class.getDeclaredFields()
		).anyMatch(field ->
				field.getType() == AccessTokenIssuer.class
						|| field.getType() == RefreshSessionIssuer.class
		);

		assertThat(hasTokenIssuerDependency).isFalse();
	}

	private RefreshSession activeSession(String userId, String tokenHash) {
		return RefreshSession.create(
				userId,
				tokenHash,
				NOW.minusSeconds(60),
				NOW.plus(Duration.ofDays(14))
		);
	}
}
