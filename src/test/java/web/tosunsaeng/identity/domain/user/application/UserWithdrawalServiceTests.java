package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.request.WithdrawRequest;
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.support.LogCapture;

class UserWithdrawalServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-07T01:23:45Z");
	private static final Instant CREATED_AT = Instant.parse("2026-08-01T00:00:00Z");
	private static final String REFRESH_VALUE = "withdrawal-service-refresh-value";
	private static final String PASSWORD = "withdrawal-service-password";

	private CurrentUserProvider currentUserProvider;
	private UserRepository userRepository;
	private RefreshSessionRepository refreshSessionRepository;
	private PasswordEncoder passwordEncoder;
	private UserWithdrawalTransactionService transactionService;
	private RefreshTokenHasher refreshTokenHasher;
	private UserWithdrawalService service;

	@BeforeEach
	void setUp() {
		currentUserProvider = mock(CurrentUserProvider.class);
		userRepository = mock(UserRepository.class);
		refreshSessionRepository = mock(RefreshSessionRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		transactionService = mock(UserWithdrawalTransactionService.class);
		refreshTokenHasher = new RefreshTokenHasher();
		service = new UserWithdrawalService(
				currentUserProvider,
				userRepository,
				refreshSessionRepository,
				refreshTokenHasher,
				passwordEncoder,
				transactionService,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void localUserWithdrawsWithOwnedSessionAndMatchingPassword() {
		User user = localUser();
		RefreshSession session = activeSession(user.getUserId());
		stubCurrentUser(user, session);
		when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
		WithdrawResponse expected = WithdrawResponse.from(user.toWithdrawnTombstone(NOW));
		when(transactionService.withdraw(
				user,
				refreshTokenHasher.hash(REFRESH_VALUE),
				NOW
		)).thenReturn(new WithdrawalTransactionResult(expected, 1));

		WithdrawResponse response;
		try (LogCapture logs = LogCapture.forClass(UserWithdrawalService.class)) {
			response = service.withdraw(new WithdrawRequest(REFRESH_VALUE, PASSWORD));
			assertThat(logs.events("user.withdrawal.completed")).singleElement()
					.satisfies(event -> {
						assertThat(event.getFormattedMessage())
								.isEqualTo("회원 탈퇴 처리가 완료되었습니다");
						assertThat(LogCapture.value(event, "outcome")).isEqualTo("withdrawn");
						assertThat(LogCapture.value(event, "accountType"))
								.isEqualTo(UserAccountType.MEMBER);
						assertThat(LogCapture.value(event, "revokedSessionCount")).isEqualTo(1);
						assertThat(LogCapture.rendered(event)).doesNotContain(
								REFRESH_VALUE,
								refreshTokenHasher.hash(REFRESH_VALUE),
								PASSWORD,
								user.getEmail(),
								user.getPasswordHash()
						);
					});
		}

		assertThat(response).isEqualTo(expected);
		verify(passwordEncoder).matches(PASSWORD, user.getPasswordHash());
		verify(transactionService).withdraw(
				user,
				refreshTokenHasher.hash(REFRESH_VALUE),
				NOW
		);
	}

	@Test
	void guestUsesAccessAndRefreshSessionOwnershipWithoutPassword() {
		User guest = guestUser();
		stubCurrentUser(guest, activeSession(guest.getUserId()));
		WithdrawResponse expected = WithdrawResponse.from(guest.toWithdrawnTombstone(NOW));
		when(transactionService.withdraw(any(), any(), any()))
				.thenReturn(new WithdrawalTransactionResult(expected, 1));

		WithdrawResponse response = service.withdraw(new WithdrawRequest(REFRESH_VALUE, null));

		assertThat(response).isEqualTo(expected);
		verify(passwordEncoder, never()).matches(any(), any());
		verify(transactionService).withdraw(
				guest,
				refreshTokenHasher.hash(REFRESH_VALUE),
				NOW
		);
	}

	@Test
	void anotherUsersRefreshSessionIsRejectedWithoutMutation() {
		User user = localUser();
		User other = guestUser();
		stubCurrentUser(user, activeSession(other.getUserId()));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(new WithdrawRequest(REFRESH_VALUE, PASSWORD))
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
		verify(passwordEncoder, never()).matches(any(), any());
		verify(transactionService, never()).withdraw(any(), any(), any());
	}

	@Test
	void memberWithoutLocalCredentialDoesNotFallIntoPasswordVerification() {
		User socialOnlyMember = mock(User.class);
		when(socialOnlyMember.getUserId()).thenReturn(
				"6fe8d7f6-12a2-4e4e-88be-04ff1e102d24"
		);
		when(socialOnlyMember.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(socialOnlyMember.getAccountType()).thenReturn(UserAccountType.MEMBER);
		when(socialOnlyMember.isMember()).thenReturn(true);
		when(socialOnlyMember.isGuest()).thenReturn(false);
		when(socialOnlyMember.hasLocalCredential()).thenReturn(false);
		stubCurrentUser(
				socialOnlyMember,
				activeSession(socialOnlyMember.getUserId())
		);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(new WithdrawRequest(REFRESH_VALUE, null))
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
		verify(passwordEncoder, never()).matches(any(), any());
		verify(transactionService, never()).withdraw(any(), any(), any());
	}

	@Test
	void missingExpiredAndRevokedRefreshSessionsUseOneSafeCredentialError() {
		User user = localUser();
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		String hash = refreshTokenHasher.hash(REFRESH_VALUE);

		when(refreshSessionRepository.findByTokenHash(hash)).thenReturn(Optional.empty());
		assertInvalidCredentials(user);

		RefreshSession expired = RefreshSession.create(
				user.getUserId(),
				hash,
				CREATED_AT,
				NOW
		);
		when(refreshSessionRepository.findByTokenHash(hash)).thenReturn(Optional.of(expired));
		assertInvalidCredentials(user);

		RefreshSession revoked = activeSession(user.getUserId());
		revoked.logout(NOW.minusSeconds(1));
		when(refreshSessionRepository.findByTokenHash(hash)).thenReturn(Optional.of(revoked));
		assertInvalidCredentials(user);

		verify(transactionService, never()).withdraw(any(), any(), any());
	}

	@Test
	void localMissingOrBlankPasswordIsBadRequestAndDoesNotMutate() {
		User user = localUser();
		stubCurrentUser(user, activeSession(user.getUserId()));

		for (String password : new String[] {null, "", "   "}) {
			BusinessException exception = catchThrowableOfType(
					BusinessException.class,
					() -> service.withdraw(new WithdrawRequest(REFRESH_VALUE, password))
			);
			assertThat(exception.getErrorCode())
					.isEqualTo(UserErrorStatus.WITHDRAWAL_PASSWORD_REQUIRED);
		}

		verify(transactionService, never()).withdraw(any(), any(), any());
	}

	@Test
	void localPasswordMismatchUsesSafeCredentialError() {
		User user = localUser();
		stubCurrentUser(user, activeSession(user.getUserId()));
		when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(false);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(new WithdrawRequest(REFRESH_VALUE, PASSWORD))
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
		verify(transactionService, never()).withdraw(any(), any(), any());
	}

	@Test
	void suspendedUserCanWithdrawWithValidCredentials() {
		User user = localUser();
		ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
		stubCurrentUser(user, activeSession(user.getUserId()));
		when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
		WithdrawResponse expected = WithdrawResponse.from(user.toWithdrawnTombstone(NOW));
		when(transactionService.withdraw(any(), any(), any()))
				.thenReturn(new WithdrawalTransactionResult(expected, 1));

		WithdrawResponse response = service.withdraw(new WithdrawRequest(REFRESH_VALUE, PASSWORD));

		assertThat(response.status()).isEqualTo(UserStatus.WITHDRAWN);
		verify(transactionService).withdraw(user, refreshTokenHasher.hash(REFRESH_VALUE), NOW);
	}

	@Test
	void alreadyWithdrawnUserReturnsOriginalTimeWithoutRevalidatingRevokedSession() {
		User withdrawn = guestUser().toWithdrawnTombstone(NOW.minusSeconds(60));
		when(currentUserProvider.getCurrentUserId()).thenReturn(withdrawn.getUserId());
		when(userRepository.findById(withdrawn.getUserId())).thenReturn(Optional.of(withdrawn));

		WithdrawResponse response = service.withdraw(
				new WithdrawRequest("any-nonblank-value", null)
		);

		assertThat(response.status()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(response.withdrawnAt()).isEqualTo(NOW.minusSeconds(60));
		verify(refreshSessionRepository, never()).findByTokenHash(any());
		verify(transactionService, never()).withdraw(any(), any(), any());
	}

	@Test
	void concurrentSuccessfulWithdrawalIsReturnedAsIdempotentSuccess() {
		User user = guestUser();
		User withdrawn = user.toWithdrawnTombstone(NOW);
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId()))
				.thenReturn(Optional.of(user), Optional.of(withdrawn));
		when(refreshSessionRepository.findByTokenHash(refreshTokenHasher.hash(REFRESH_VALUE)))
				.thenReturn(Optional.of(activeSession(user.getUserId())));
		when(transactionService.withdraw(any(), any(), any()))
				.thenThrow(new web.tosunsaeng.identity.domain.user.exception.UserException(
						UserErrorStatus.WITHDRAWAL_CONFLICT
				));

		WithdrawResponse response = service.withdraw(new WithdrawRequest(REFRESH_VALUE, null));

		assertThat(response.status()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(response.withdrawnAt()).isEqualTo(NOW);
	}

	@Test
	void oneConcurrentUpdateConflictIsRetriedWithFreshUserState() {
		User user = guestUser();
		stubCurrentUser(user, activeSession(user.getUserId()));
		WithdrawResponse expected = WithdrawResponse.from(user.toWithdrawnTombstone(NOW));
		when(transactionService.withdraw(
				user,
				refreshTokenHasher.hash(REFRESH_VALUE),
				NOW
		)).thenThrow(new web.tosunsaeng.identity.domain.user.exception.UserException(
				UserErrorStatus.WITHDRAWAL_CONFLICT
		)).thenReturn(new WithdrawalTransactionResult(expected, 1));

		WithdrawResponse response = service.withdraw(new WithdrawRequest(REFRESH_VALUE, null));

		assertThat(response).isEqualTo(expected);
		verify(userRepository, times(2)).findById(user.getUserId());
		verify(transactionService, times(2)).withdraw(
				user,
				refreshTokenHasher.hash(REFRESH_VALUE),
				NOW
		);
	}

	@Test
	void missingJwtSubjectUserUsesExistingNotFoundContract() {
		when(currentUserProvider.getCurrentUserId()).thenReturn("missing-user-id");
		when(userRepository.findById("missing-user-id")).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(new WithdrawRequest(REFRESH_VALUE, PASSWORD))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.USER_NOT_FOUND);
	}

	@Test
	void requestContainsNoTargetSelectorAndRedactsCredentials() {
		assertThat(Arrays.stream(WithdrawRequest.class.getRecordComponents())
				.map(RecordComponent::getName))
				.containsExactly("refreshToken", "password")
				.doesNotContain("userId", "installationId", "isConfirmed");
		assertThat(new WithdrawRequest(REFRESH_VALUE, PASSWORD).toString())
				.isEqualTo("WithdrawRequest[redacted]")
				.doesNotContain(REFRESH_VALUE, PASSWORD);
	}

	private void assertInvalidCredentials(User user) {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(new WithdrawRequest(REFRESH_VALUE, PASSWORD))
		);
		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	private void stubCurrentUser(User user, RefreshSession session) {
		String userId = user.getUserId();
		when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(refreshSessionRepository.findByTokenHash(refreshTokenHasher.hash(REFRESH_VALUE)))
				.thenReturn(Optional.of(session));
	}

	private User localUser() {
		return User.create(
				"withdrawal.user@example.test",
				"withdrawal.user@example.test",
				"encoded-withdrawal-password",
				"탈퇴테스트",
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

	private RefreshSession activeSession(String userId) {
		return RefreshSession.create(
				userId,
				refreshTokenHasher.hash(REFRESH_VALUE),
				CREATED_AT,
				NOW.plusSeconds(3600)
		);
	}
}
