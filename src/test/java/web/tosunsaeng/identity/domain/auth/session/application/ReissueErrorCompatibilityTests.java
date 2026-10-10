package web.tosunsaeng.identity.domain.auth.session.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import web.tosunsaeng.identity.domain.auth.common.api.AuthController;
import web.tosunsaeng.identity.domain.auth.local.application.LoginService;
import web.tosunsaeng.identity.domain.auth.registration.application.*;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.ReissueNoStoreFilter;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.*;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

class ReissueErrorCompatibilityTests {

	final ReissueRequest request = new ReissueRequest("test-only-refresh");
	final List<String> ids = List.of("11111111-1111-4111-8111-111111111111");
	final RefreshTokenHasher hasher = new RefreshTokenHasher();
	RefreshSessionRepository sessions;
	UserRepository users;
	RefreshSessionIssuer issuer;
	AccessTokenIssuer access;
	ReissueRecoveryService recovery;
	RefreshSession session;
	User user;
	TokenReissueService service;

	@BeforeEach void setup() {
		sessions = mock(RefreshSessionRepository.class);
		users = mock(UserRepository.class);
		issuer = mock(RefreshSessionIssuer.class);
		access = mock(AccessTokenIssuer.class);
		recovery = mock(ReissueRecoveryService.class);
		session = mock(RefreshSession.class);
		user = mock(User.class);
		when(session.getUserId()).thenReturn("00000000-0000-4000-8000-000000000001");
		when(sessions.findByTokenHash(hasher.hash(request.refreshToken()))).thenReturn(Optional.of(session));
		when(users.findById(session.getUserId())).thenReturn(Optional.of(user));
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(user.getAccountType()).thenReturn(UserAccountType.GUEST);
		service = new TokenReissueService(hasher, sessions, users, access, issuer, new AuthResponseConverter(), Clock.systemUTC());
		service.setRecovery(recovery);
	}

	@ParameterizedTest @ValueSource(booleans = {true, false})
	void mapsBothEntryPointsWithoutIssuingOrSaving(boolean headers) {
		when(recovery.reissue(request, headers ? ids : List.of()))
				.thenThrow(AuthException.loggedOut(SessionRejectionReason.SOURCE_REVOKED));
		assertThatThrownBy(() -> { if (headers) service.reissue(request, ids); else service.reissue(request); })
				.isInstanceOfSatisfying(AuthException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN);
					assertThat(e.getErrorCode().getHttpStatus().value()).isEqualTo(401);
				});
		verifyNoInteractions(access, issuer);
		verify(sessions, never()).save(any());
	}

	@ParameterizedTest @EnumSource(SessionRejectionReason.class)
	void preservesAllInternalRejectionReasonsWhileMappingResponse(SessionRejectionReason reason) {
		when(recovery.reissue(request, ids)).thenThrow(AuthException.loggedOut(reason));
		assertThatThrownBy(() -> service.reissue(request, ids)).isInstanceOfSatisfying(AuthException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN));
	}

	@Test void mappingDoesNotRequireAdditionalDatabaseLookups() {
		when(recovery.reissue(request, ids)).thenThrow(AuthException.loggedOut(SessionRejectionReason.SOURCE_REVOKED));
		clearInvocations(sessions, users);
		assertThatThrownBy(() -> service.reissue(request, ids)).isInstanceOfSatisfying(AuthException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN));
		verifyNoInteractions(sessions, users);
	}
	@ParameterizedTest @EnumSource(value = AuthErrorStatus.class, names = {
			"REFRESH_TOKEN_REUSE_DETECTED", "REFRESH_TOKEN_EXPIRED", "ACCOUNT_WITHDRAWN",
			"ACCOUNT_MERGED_TOKEN_REJECTED", "SESSION_SECURITY_UNAVAILABLE", "INVALID_REFRESH_TOKEN"})
	void otherErrorsRemainUnchanged(AuthErrorStatus status) {
		var original = new AuthException(status);
		when(recovery.reissue(request, ids)).thenThrow(original);
		clearInvocations(sessions, users);
		assertThatThrownBy(() -> service.reissue(request, ids)).isSameAs(original);
		verifyNoInteractions(sessions, users);
	}
	@Test void successfulResponseIsUnchanged() {
		var result = new ReissueResult(null, null, null, null);
		when(recovery.reissue(request, ids)).thenReturn(result);
		assertThat(service.reissue(request, ids)).isSameAs(result);
	}
	@ParameterizedTest @ValueSource(booleans = {true, false})
	void httpResponseUsesExpectedCodeAndNoStore(boolean guest) throws Exception {
		when(user.getAccountType()).thenReturn(guest ? UserAccountType.GUEST : UserAccountType.MEMBER);
		when(recovery.reissue(any(), eq(ids))).thenThrow(AuthException.loggedOut(SessionRejectionReason.SOURCE_REVOKED));
		var controller = new AuthController(mock(EmailAvailabilityService.class), mock(SignupService.class),
				mock(GuestAuthService.class), mock(LoginService.class), service, mock(LogoutService.class), mock(LogoutAllService.class));
		var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
				.addFilters(new ReissueNoStoreFilter()).build();
		mvc.perform(post("/api/v1/auth/reissue").header("Idempotency-Key", ids.get(0))
				.contentType("application/json").content("{\"refreshToken\":\"test-only-refresh\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().doesNotExist("Reissue-Access-Expires-At"));
		verifyNoInteractions(access, issuer);
	}
	@Test void legacyFenceRejectionAlsoMapsWithoutIssuing() {
		service.setRecovery(null);
		var security = mock(SessionSecurityService.class);
		when(issuer.isFenceEnabled()).thenReturn(true);
		when(issuer.security()).thenReturn(security);
		when(security.transaction(any())).thenThrow(AuthException.loggedOut(SessionRejectionReason.EPOCH_MISMATCH));
		assertThatThrownBy(() -> service.reissue(request, ids)).isInstanceOfSatisfying(AuthException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.INVALID_REFRESH_TOKEN));
		verifyNoInteractions(access);
		verify(sessions, never()).save(any());
	}
}
