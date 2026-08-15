package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import web.tosunsaeng.identity.domain.auth.application.GuestAuthService;
import web.tosunsaeng.identity.domain.auth.application.GuestRegistrationTransactionService;
import web.tosunsaeng.identity.domain.auth.application.LoginService;
import web.tosunsaeng.identity.domain.auth.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.application.SignupService;
import web.tosunsaeng.identity.domain.auth.application.TokenReissueService;
import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.dto.request.GuestAuthRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.LoginRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.request.WithdrawRequest;
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.guest.GuestInstallationIdHasher;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenGenerator;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenProperties;

class UserWithdrawalLifecycleTests {

	private static final Instant NOW = Instant.parse("2026-08-07T01:23:45Z");
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);
	private static final String INSTALLATION_ID = "550e8400-e29b-41d4-a716-446655440000";
	private static final String FIRST_REFRESH = "first-guest-lifecycle-refresh-value";
	private static final String SECOND_REFRESH = "second-guest-lifecycle-refresh-value";
	private static final String THIRD_REFRESH = "third-guest-lifecycle-refresh-value";

	private final Map<String, User> users = new LinkedHashMap<>();
	private final Map<String, RefreshSession> sessions = new LinkedHashMap<>();
	private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();
	private final GuestInstallationIdHasher installationIdHasher = new GuestInstallationIdHasher();

	private UserRepository userRepository;
	private RefreshSessionRepository sessionRepository;
	private Clock clock;
	private ConsentPolicy consentPolicy;
	private UserFactory userFactory;
	private RefreshSessionIssuer refreshSessionIssuer;
	private AccessTokenIssuer accessTokenIssuer;
	private GuestAuthService guestAuthService;
	private CurrentUserProvider currentUserProvider;
	private UserWithdrawalService withdrawalService;
	private TokenReissueService reissueService;

	@BeforeEach
	void setUp() {
		userRepository = statefulUserRepository();
		sessionRepository = statefulSessionRepository();
		clock = Clock.fixed(NOW, ZoneOffset.UTC);
		consentPolicy = new ConsentPolicy(
				"privacy-v1",
				"term-v1",
				"quality-review-v1"
		);
		userFactory = new UserFactory(
				new EmailNormalizer(),
				new BCryptPasswordEncoder(4),
				consentPolicy
		);
		Queue<String> tokenValues = new ArrayDeque<>(List.of(
				FIRST_REFRESH,
				SECOND_REFRESH,
				THIRD_REFRESH
		));
		RefreshTokenGenerator tokenGenerator = mock(RefreshTokenGenerator.class);
		when(tokenGenerator.generate()).thenAnswer(invocation -> tokenValues.remove());
		refreshSessionIssuer = new RefreshSessionIssuer(
				tokenGenerator,
				refreshTokenHasher,
				sessionRepository,
				new RefreshTokenProperties(REFRESH_TTL, 32),
				clock
		);
		accessTokenIssuer = accessTokenIssuer();
		AuthResponseConverter responseConverter = new AuthResponseConverter();
		GuestRegistrationTransactionService guestTransactionService =
				new GuestRegistrationTransactionService(
						userRepository,
						refreshSessionIssuer,
						responseConverter
				);
		guestAuthService = new GuestAuthService(
				userRepository,
				userFactory,
				consentPolicy,
				installationIdHasher,
				accessTokenIssuer,
				refreshSessionIssuer,
				guestTransactionService,
				clock
		);

		currentUserProvider = mock(CurrentUserProvider.class);
		UserWithdrawalTransactionService withdrawalTransactionService =
				new UserWithdrawalTransactionService(userRepository, sessionRepository);
		withdrawalService = new UserWithdrawalService(
				currentUserProvider,
				userRepository,
				sessionRepository,
				refreshTokenHasher,
				new BCryptPasswordEncoder(4),
				withdrawalTransactionService,
				clock
		);
		reissueService = new TokenReissueService(
				refreshTokenHasher,
				sessionRepository,
				userRepository,
				accessTokenIssuer,
				refreshSessionIssuer,
				responseConverter,
				clock
		);
	}

	@Test
	void withdrawnGuestRejoinsSameInstallationAsNewUserWithNewSession() {
		GuestAuthRequest request = guestRequest();
		GuestAuthResponse firstAuth = guestAuthService.authenticate(request);
		User firstUser = users.values().iterator().next();
		String installationHash = installationIdHasher.hash(INSTALLATION_ID);
		when(currentUserProvider.getCurrentUserId()).thenReturn(firstUser.getUserId());

		WithdrawResponse withdrawal = withdrawalService.withdraw(
				new WithdrawRequest(firstAuth.refreshToken(), null)
		);
		GuestAuthResponse secondAuth = guestAuthService.authenticate(request);

		User persistedFirstUser = users.get(firstUser.getUserId());
		User secondUser = users.values().stream()
				.filter(user -> !user.getUserId().equals(firstUser.getUserId()))
				.findFirst()
				.orElseThrow();
		assertThat(withdrawal.status()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(persistedFirstUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(persistedFirstUser.getGuestInstallationIdHash()).isNull();
		assertThat(secondUser.getUserId()).isNotEqualTo(firstUser.getUserId());
		assertThat(secondUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(secondUser.getGuestInstallationIdHash()).isEqualTo(installationHash);
		assertThat(secondAuth.refreshToken()).isEqualTo(SECOND_REFRESH);
		assertThat(sessions.values().stream()
				.filter(session -> session.getUserId().equals(firstUser.getUserId())))
				.allSatisfy(session -> assertThat(session.getRevocationReason())
						.isEqualTo(RevocationReason.ACCOUNT_WITHDRAWN));
		assertThat(sessions.values().stream()
				.filter(session -> session.getUserId().equals(secondUser.getUserId())))
				.singleElement()
				.satisfies(session -> {
					assertThat(session.isRevoked()).isFalse();
					assertThat(session.getTokenHash())
							.isEqualTo(refreshTokenHasher.hash(SECOND_REFRESH));
				});
	}

	@Test
	void allPreWithdrawalRefreshTokensFailAndActiveGuestDuplicatePolicyRemains() {
		GuestAuthRequest request = guestRequest();
		GuestAuthResponse firstAuth = guestAuthService.authenticate(request);
		User firstUser = users.values().iterator().next();
		when(currentUserProvider.getCurrentUserId()).thenReturn(firstUser.getUserId());
		withdrawalService.withdraw(new WithdrawRequest(firstAuth.refreshToken(), null));

		BusinessException reissueFailure = catchThrowableOfType(
				BusinessException.class,
				() -> reissueService.reissue(new ReissueRequest(firstAuth.refreshToken()))
		);
		assertThat(reissueFailure.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN);

		guestAuthService.authenticate(request);
		BusinessException duplicate = catchThrowableOfType(
				BusinessException.class,
				() -> guestAuthService.authenticate(request)
		);
		assertThat(duplicate.getErrorCode()).isEqualTo(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		assertThat(users).hasSize(2);
		assertThat(sessions.values().stream()
				.filter(session -> session.getUserId().equals(firstUser.getUserId())))
				.allMatch(RefreshSession::isRevoked);
	}

	@Test
	void withdrawnLocalUserCanSignUpAgainWithSameEmailAsNewUser() {
		EmailNormalizer emailNormalizer = new EmailNormalizer();
		SignupService signupService = new SignupService(
				userRepository,
				emailNormalizer,
				userFactory,
				consentPolicy
		);
		SignupRequest signupRequest = new SignupRequest(
				"rejoin.user@example.test",
				"RejoinPassword!23",
				"첫번째사용자",
				true,
				"privacy-v1",
				true,
				"term-v1"
		);
		SignupResponse firstSignup = signupService.signup(signupRequest);
		LoginService loginService = new LoginService(
				userRepository,
				emailNormalizer,
				new BCryptPasswordEncoder(4),
				accessTokenIssuer,
				refreshSessionIssuer,
				new AuthResponseConverter()
		);
		LoginResponse login = loginService.login(new LoginRequest(
				"rejoin.user@example.test",
				"RejoinPassword!23"
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(firstSignup.userId());

		withdrawalService.withdraw(new WithdrawRequest(
				login.refreshToken(),
				"RejoinPassword!23"
		));
		SignupResponse secondSignup = signupService.signup(new SignupRequest(
				"rejoin.user@example.test",
				"RejoinPassword!23",
				"두번째사용자",
				true,
				"privacy-v1",
				true,
				"term-v1"
		));

		User firstUser = users.get(firstSignup.userId());
		User secondUser = users.get(secondSignup.userId());
		assertThat(secondSignup.userId()).isNotEqualTo(firstSignup.userId());
		assertThat(firstUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(firstUser.getEmail()).isNull();
		assertThat(firstUser.getNormalizedEmail()).isNull();
		assertThat(firstUser.getPasswordHash()).isNull();
		assertThat(secondUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(secondUser.getNormalizedEmail()).isEqualTo("rejoin.user@example.test");
	}

	@Test
	void withdrawalRevokesThreeExistingSessionsAndEveryTokenFailsReissue() {
		EmailNormalizer emailNormalizer = new EmailNormalizer();
		SignupService signupService = new SignupService(
				userRepository,
				emailNormalizer,
				userFactory,
				consentPolicy
		);
		SignupResponse signup = signupService.signup(new SignupRequest(
				"sessions.user@example.test",
				"SessionPassword!23",
				"세션사용자",
				true,
				"privacy-v1",
				true,
				"term-v1"
		));
		LoginService loginService = new LoginService(
				userRepository,
				emailNormalizer,
				new BCryptPasswordEncoder(4),
				accessTokenIssuer,
				refreshSessionIssuer,
				new AuthResponseConverter()
		);
		LoginRequest loginRequest = new LoginRequest(
				"sessions.user@example.test",
				"SessionPassword!23"
		);
		List<String> refreshTokens = List.of(
				loginService.login(loginRequest).refreshToken(),
				loginService.login(loginRequest).refreshToken(),
				loginService.login(loginRequest).refreshToken()
		);
		when(currentUserProvider.getCurrentUserId()).thenReturn(signup.userId());

		WithdrawResponse response = withdrawalService.withdraw(new WithdrawRequest(
				refreshTokens.getFirst(),
				"SessionPassword!23"
		));

		assertThat(response.status()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(sessions.values().stream()
				.filter(session -> session.getUserId().equals(signup.userId())))
				.hasSize(3)
				.allSatisfy(session -> {
					assertThat(session.getRevocationReason())
							.isEqualTo(RevocationReason.ACCOUNT_WITHDRAWN);
					assertThat(session.getRevokedAt()).isEqualTo(NOW);
				});
		refreshTokens.forEach(refreshToken -> {
			BusinessException exception = catchThrowableOfType(
					BusinessException.class,
					() -> reissueService.reissue(new ReissueRequest(refreshToken))
			);
			assertThat(exception.getErrorCode()).isEqualTo(
					AuthErrorStatus.INVALID_REFRESH_TOKEN
			);
		});
	}

	private UserRepository statefulUserRepository() {
		UserRepository repository = mock(UserRepository.class);
		when(repository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			users.put(user.getUserId(), user);
			return user;
		});
		when(repository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(
				users.get(invocation.getArgument(0))
		));
		when(repository.existsByGuestInstallationIdHash(any())).thenAnswer(invocation -> {
			String hash = invocation.getArgument(0);
			return users.values().stream()
					.anyMatch(user -> hash.equals(user.getGuestInstallationIdHash()));
		});
		when(repository.existsByNormalizedEmail(any())).thenAnswer(invocation -> {
			String normalizedEmail = invocation.getArgument(0);
			return users.values().stream()
					.anyMatch(user -> normalizedEmail.equals(user.getNormalizedEmail()));
		});
		when(repository.findByNormalizedEmail(any())).thenAnswer(invocation -> {
			String normalizedEmail = invocation.getArgument(0);
			return users.values().stream()
					.filter(user -> normalizedEmail.equals(user.getNormalizedEmail()))
					.findFirst();
		});
		when(repository.withdrawIfUnchanged(any(), any(), any())).thenAnswer(invocation -> {
			User tombstone = invocation.getArgument(0);
			UserStatus expectedStatus = invocation.getArgument(1);
			Instant expectedUpdatedAt = invocation.getArgument(2);
			User current = users.get(tombstone.getUserId());
			if (current == null
					|| current.getStatus() != expectedStatus
					|| !java.util.Objects.equals(current.getUpdatedAt(), expectedUpdatedAt)) {
				return false;
			}
			users.put(tombstone.getUserId(), tombstone);
			return true;
		});
		return repository;
	}

	private RefreshSessionRepository statefulSessionRepository() {
		RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
		when(repository.save(any(RefreshSession.class))).thenAnswer(invocation -> {
			RefreshSession session = invocation.getArgument(0);
			sessions.put(session.getSessionId(), session);
			return session;
		});
		when(repository.saveAll(any())).thenAnswer(invocation -> {
			List<RefreshSession> saved = invocation.getArgument(0);
			saved.forEach(session -> sessions.put(session.getSessionId(), session));
			return saved;
		});
		when(repository.findByTokenHash(any())).thenAnswer(invocation -> {
			String hash = invocation.getArgument(0);
			return sessions.values().stream()
					.filter(session -> session.getTokenHash().equals(hash))
					.findFirst();
		});
		when(repository.findAllByUserIdAndRevokedAtIsNull(any())).thenAnswer(invocation -> {
			String userId = invocation.getArgument(0);
			return sessions.values().stream()
					.filter(session -> session.getUserId().equals(userId))
					.filter(session -> session.getRevokedAt() == null)
					.toList();
		});
		return repository;
	}

	private AccessTokenIssuer accessTokenIssuer() {
		AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);
		AtomicInteger sequence = new AtomicInteger();
		when(issuer.issue(any(), eq(Set.of()))).thenAnswer(invocation -> new IssuedAccessToken(
				"guest-lifecycle-access-value-" + sequence.incrementAndGet(),
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				NOW,
				NOW.plus(ACCESS_TTL),
				ACCESS_TTL.toSeconds()
		));
		return issuer;
	}

	private GuestAuthRequest guestRequest() {
		return new GuestAuthRequest(
				INSTALLATION_ID,
				true,
				"privacy-v1",
				true,
				"term-v1",
				false,
				"quality-review-v1"
		);
	}
}
