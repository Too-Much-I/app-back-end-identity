package web.tosunsaeng.identity.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.nimbusds.jose.jwk.RSAKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.global.security.jwt.JwtConfiguration;
import web.tosunsaeng.identity.global.security.jwt.JwtProperties;
import web.tosunsaeng.identity.global.security.jwt.TestRsaKeyConfiguration;
import web.tosunsaeng.identity.global.observability.RequestLoggingFilter;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalService;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
		TestRsaKeyConfiguration.class,
		SecurityIntegrationTests.TestEndpointConfiguration.class
})
class SecurityIntegrationTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String OTHER_USER_ID = "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private UserFactory userFactory;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private RefreshSessionRepository refreshSessionRepository;

	@MockitoBean
	private UserWithdrawnOutboxRepository userWithdrawnOutboxRepository;

	@MockitoBean
	private UserWithdrawalService userWithdrawalService;

	@ParameterizedTest
	@ValueSource(strings = {
			"/api/v1/auth/check-email",
			"/api/v1/auth/signup",
			"/api/v1/auth/guest",
			"/api/v1/auth/firebase/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/reissue",
			"/api/v1/auth/logout"
	})
	void publicAuthenticationEndpointsReachMvcValidationWithoutAccessToken(String path) throws Exception {
		mockMvc.perform(post(path)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void disabledFirebaseExchangeIsPublicAndReturnsStableUnavailableResponse() throws Exception {
		mockMvc.perform(post("/api/v1/auth/firebase/exchange")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"firebaseIdToken\":\"test-only-credential\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.code").value("FIREBASE_UNAVAILABLE"));
	}

	@Test
	void disabledFirebaseSignupIsPublicAndReturnsStableUnavailableResponse() throws Exception {
		mockMvc.perform(post("/api/v1/auth/firebase/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
								  "firebaseIdToken": "test-only-credential",
								  "nickname": "테스트회원",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.code").value("FIREBASE_UNAVAILABLE"));
	}

	@Test
	void firebaseExchangeOpenApiMarksCredentialWriteOnlyAndDocumentsUnionResult() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseExchangeRequest.properties.firebaseIdToken.writeOnly"
				).value(true))
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseExchangeResponseEnvelope.properties.result.$ref"
				).value("#/components/schemas/FirebaseExchangeResponse"))
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseExchangeResponse.oneOf[*].$ref",
						hasItems(
								"#/components/schemas/FirebaseAuthenticatedResponse",
								"#/components/schemas/FirebaseEnrollmentRequiredResponse"
						)
				))
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/firebase/exchange'].post.security"
				).doesNotExist());
	}

	@Test
	void firebaseSignupOpenApiIsPublicAndDoesNotAcceptUserIdOrPhone() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseSignupRequest.properties.firebaseIdToken.writeOnly"
				).value(true))
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseSignupRequest.properties.userId"
				).doesNotExist())
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseSignupRequest.properties.phone"
				).doesNotExist())
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/firebase/signup'].post.security"
				).doesNotExist());
	}

	@Test
	void guestUpgradeAndAuthMethodOpenApiRequireBearerAndNeverAcceptUserId() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/firebase/guest/prepare'].post.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/firebase/guest/upgrade'].post.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/firebase/guest/merge'].post.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/firebase/auth-methods/sync'].post.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseGuestPrepareRequest.properties.firebaseIdToken.writeOnly"
				).value(true))
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseGuestUpgradeRequest.properties.firebaseIdToken.writeOnly"
				).value(true))
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseGuestMergeRequest.properties.firebaseIdToken.writeOnly"
				).value(true))
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseGuestMergeRequest.properties.userId"
				).doesNotExist())
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseAuthMethodsSyncRequest.properties.firebaseIdToken.writeOnly"
				).value(true))
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseGuestPrepareRequest.properties.userId"
				).doesNotExist())
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseGuestUpgradeRequest.properties.userId"
				).doesNotExist())
				.andExpect(jsonPath(
						"$.components.schemas.FirebaseAuthMethodsSyncRequest.properties.userId"
				).doesNotExist());
	}

	@Test
	void malformedJsonReturnsSafeBadRequestWithoutEchoingRequestContent() throws Exception {
		String requestOnlySensitiveValue = "request-only-sensitive-value";
		String malformedJson = "{\"password\":\"%s\"".formatted(requestOnlySensitiveValue);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(malformedJson))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain(
				requestOnlySensitiveValue,
				"HttpMessageNotReadableException",
				"JsonEOFException",
				"stackTrace"
		);
	}

	@Test
	void authenticatedUnknownApiReturnsSafeNotFoundResponse() throws Exception {
		IssuedAccessToken accessToken = accessTokenIssuer.issue(USER_ID, Set.of());

		MvcResult result = mockMvc.perform(get("/api/v1/not-found")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.tokenValue()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain(
				accessToken.tokenValue(),
				"NoResourceFoundException",
				"stackTrace"
		);
	}

	@Test
	void authenticatedUnsupportedMethodReturns405() throws Exception {
		IssuedAccessToken accessToken = accessTokenIssuer.issue(USER_ID, Set.of());

		mockMvc.perform(get("/api/v1/auth/signup")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.tokenValue()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
				.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void unsupportedMediaTypeReturns415() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.TEXT_PLAIN)
						.content("not-json"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
				.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void jwksHealthSwaggerAndOpenApiRemainPublic() throws Exception {
		mockMvc.perform(get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/reissue'].post.responses['401'].description"
				).value(containsString("ACCOUNT_WITHDRAWN")))
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/swagger-ui/index.html"))
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
	}

	@Test
	void protectedEndpointsReturnSafeBaseResponseWhenAccessTokenIsMissing() throws Exception {
		assertUnauthorized(mockMvc.perform(get("/api/v1/users/me")).andReturn());
		assertUnauthorized(mockMvc.perform(get("/api/v1/users/me/consents")).andReturn());
		assertUnauthorized(mockMvc.perform(put("/api/v1/users/me/consents")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andReturn());
		assertUnauthorized(mockMvc.perform(post("/api/v1/auth/logout-all")).andReturn());
		assertUnauthorized(mockMvc.perform(post("/api/v1/users/withdraw")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"refreshToken\":\"security-test-value\"}"))
				.andReturn());
		assertUnauthorized(mockMvc.perform(post("/api/v1/auth/firebase/guest/prepare")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andReturn());
		assertUnauthorized(mockMvc.perform(post("/api/v1/auth/firebase/guest/upgrade")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andReturn());
		assertUnauthorized(mockMvc.perform(post("/api/v1/auth/firebase/auth-methods/sync")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andReturn());
	}

	@Test
	void requestLoggingFilterWrapsSecurityFailureAndEchoesSafeRequestId() throws Exception {
		String requestId = "security-request-123";

		mockMvc.perform(get("/api/v1/users/me")
						.header(RequestLoggingFilter.REQUEST_ID_HEADER, requestId))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(RequestLoggingFilter.REQUEST_ID_HEADER, requestId))
				.andExpect(jsonPath("$.code").value("COMMON_UNAUTHORIZED"));
	}

	@Test
	void withdrawalEndpointRequiresJwtAndAcceptsValidAccessToken() throws Exception {
		IssuedAccessToken accessToken = accessTokenIssuer.issue(USER_ID, Set.of());
		when(userWithdrawalService.withdraw(any()))
				.thenReturn(new WithdrawResponse(
						UserStatus.WITHDRAWN,
						TestRsaKeyConfiguration.TEST_INSTANT
				));

		mockMvc.perform(post("/api/v1/users/withdraw")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.tokenValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"security-test-value\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.status").value("WITHDRAWN"));
	}

	@Test
	void withdrawalEndpointRejectsMalformedBearerToken() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/users/withdraw")
						.header(HttpHeaders.AUTHORIZATION, "Bearer malformed-test-value")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"security-test-value\"}"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertUnauthorized(result);
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("malformed-test-value");
	}

	@Test
	void formLoginAndHttpBasicAreNotEnabled() throws Exception {
		MvcResult loginPage = mockMvc.perform(get("/login"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(header().doesNotExist(HttpHeaders.LOCATION))
				.andReturn();
		assertUnauthorized(loginPage);

		MvcResult basic = mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Basic dGVzdDp0ZXN0"))
				.andExpect(status().isUnauthorized())
				.andReturn();
		assertUnauthorized(basic);
		assertThat(basic.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
				.isNull();
	}

	@Test
	void validRs256TokenReturnsCurrentUsersSafeProfileUsingJwtSubject() throws Exception {
		User user = userFactory.create(
				"Profile.User@Example.com",
				"integration-test-credential",
				"토스마스터"
		);
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getUserId(), Set.of());

		MvcResult result = mockMvc.perform(get("/api/v1/users/me")
						.queryParam("userId", OTHER_USER_ID)
						.header(
								HttpHeaders.AUTHORIZATION,
								"Bearer " + accessToken.tokenValue()
						))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.userId").value(user.getUserId()))
				.andExpect(jsonPath("$.result.email").value("Profile.User@Example.com"))
				.andExpect(jsonPath("$.result.nickname").value("토스마스터"))
				.andExpect(jsonPath("$.result.accountType").value("MEMBER"))
				.andExpect(jsonPath("$.result.provider").value("LOCAL"))
				.andExpect(jsonPath("$.result.privacyConsented").value(true))
				.andExpect(jsonPath("$.result.privacyConsentVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.privacyConsentedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.termConsented").value(true))
				.andExpect(jsonPath("$.result.termConsentVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.termConsentedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.result.password").doesNotExist())
				.andExpect(jsonPath("$.result.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.result.normalizedEmail").doesNotExist())
				.andExpect(jsonPath("$.result.refreshSession").doesNotExist())
				.andExpect(jsonPath("$.result.tokenHash").doesNotExist())
				.andExpect(jsonPath("$.result.accessToken").doesNotExist())
				.andExpect(jsonPath("$.result.refreshToken").doesNotExist())
				.andExpect(jsonPath("$.result.streakDays").doesNotExist())
				.andReturn();

		verify(userRepository).findById(user.getUserId());
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(
						user.getPasswordHash(),
						user.getNormalizedEmail(),
						accessToken.tokenValue(),
						OTHER_USER_ID
				);
	}

	@Test
	void guestRs256TokenAccessesProtectedProfileWithNullEmail() throws Exception {
		User guest = userFactory.createGuest(
				"A".repeat(43),
				TestRsaKeyConfiguration.TEST_INSTANT
		);
		when(userRepository.findById(guest.getUserId())).thenReturn(Optional.of(guest));
		IssuedAccessToken accessToken = accessTokenIssuer.issue(guest.getUserId(), Set.of());

		MvcResult result = mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.tokenValue()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.userId").value(guest.getUserId()))
				.andExpect(jsonPath("$.result.email").value(nullValue()))
				.andExpect(jsonPath("$.result.nickname").value("게스트"))
				.andExpect(jsonPath("$.result.accountType").value("GUEST"))
				.andExpect(jsonPath("$.result.provider").value("GUEST"))
				.andExpect(jsonPath("$.result.privacyConsented").value(true))
				.andExpect(jsonPath("$.result.termConsented").value(true))
				.andExpect(jsonPath("$.result.installationId").doesNotExist())
				.andExpect(jsonPath("$.result.guestInstallationIdHash").doesNotExist())
				.andExpect(jsonPath("$.result.accessToken").doesNotExist())
				.andExpect(jsonPath("$.result.refreshToken").doesNotExist())
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(guest.getGuestInstallationIdHash(), accessToken.tokenValue());
	}

	@Test
	void consentStatusReadUsesJwtSubjectAndCannotReadAnotherUser() throws Exception {
		User user = userFactory.create(
				"consent.reader@example.com",
				"integration-test-credential",
				"동의조회사용자"
		);
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getUserId(), Set.of());

		MvcResult result = mockMvc.perform(get("/api/v1/users/me/consents")
						.queryParam("userId", OTHER_USER_ID)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.tokenValue()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.privacy.currentVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.privacy.consented").value(true))
				.andExpect(jsonPath("$.result.privacy.consentedVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.privacy.requiresConsent").value(false))
				.andExpect(jsonPath("$.result.terms.currentVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.terms.consented").value(true))
				.andExpect(jsonPath("$.result.terms.consentedVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.terms.requiresConsent").value(false))
				.andExpect(jsonPath("$.result.userId").doesNotExist())
				.andExpect(jsonPath("$.result.installationId").doesNotExist())
				.andReturn();

		verify(userRepository).findById(user.getUserId());
		assertThat(result.getResponse().getContentAsString()).doesNotContain(
				OTHER_USER_ID,
				user.getPasswordHash(),
				user.getNormalizedEmail(),
				accessToken.tokenValue()
		);
	}

	@Test
	void consentUpdateUsesJwtSubjectAndCannotModifyAnotherUser() throws Exception {
		User user = userFactory.create(
				"consent.user@example.com",
				"integration-test-credential",
				"동의사용자"
		);
		ReflectionTestUtils.setField(user, "consents", null);
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		when(userRepository.updateConsentsIfActive(any(), any())).thenReturn(true);
		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getUserId(), Set.of());

		MvcResult result = mockMvc.perform(put("/api/v1/users/me/consents")
						.queryParam("userId", OTHER_USER_ID)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.tokenValue())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.privacyConsented").value(true))
				.andExpect(jsonPath("$.result.privacyConsentVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.termConsented").value(true))
				.andExpect(jsonPath("$.result.termConsentVersion").value("term-v1"))
				.andReturn();

		verify(userRepository).findById(user.getUserId());
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(OTHER_USER_ID, accessToken.tokenValue());
	}

	@Test
	void logoutAllUsesJwtSubjectAndReturnsNoSessionOrTokenData() throws Exception {
		when(refreshSessionRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID))
				.thenReturn(List.of());
		IssuedAccessToken accessToken = accessTokenIssuer.issue(USER_ID, Set.of());

		mockMvc.perform(post("/api/v1/auth/logout-all")
						.header(
								HttpHeaders.AUTHORIZATION,
								"Bearer " + accessToken.tokenValue()
						))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andExpect(jsonPath("$.sessionId").doesNotExist())
				.andExpect(jsonPath("$.tokenHash").doesNotExist())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.refreshToken").doesNotExist());

		verify(refreshSessionRepository).findAllByUserIdAndRevokedAtIsNull(USER_ID);
	}

	@Test
	void scopeClaimMapsToScopePrefixedAuthoritiesWithoutEnforcingEndpointScope() throws Exception {
		IssuedAccessToken accessToken = accessTokenIssuer.issue(
				USER_ID,
				Set.of("profile:read", "profile:write")
		);

		mockMvc.perform(get("/__security-test/authorities")
						.header(
								HttpHeaders.AUTHORIZATION,
								"Bearer " + accessToken.tokenValue()
						))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorities", hasItems(
						"SCOPE_profile:read",
						"SCOPE_profile:write"
				)));
	}

	@Test
	void badSignatureExpiredIssuerAudienceAndMissingSubjectTokensReturn401() throws Exception {
		JwtEncoder differentEncoder = differentKeyEncoder();
		List<String> invalidTokens = List.of(
				signedToken(differentEncoder, USER_ID, jwtProperties.issuer(),
						jwtProperties.audience(), TestRsaKeyConfiguration.TEST_INSTANT.plusSeconds(60)),
				signedToken(jwtEncoder, USER_ID, jwtProperties.issuer(),
						jwtProperties.audience(), TestRsaKeyConfiguration.TEST_INSTANT.minusSeconds(1)),
				signedToken(jwtEncoder, USER_ID, "https://wrong-issuer.test",
						jwtProperties.audience(), TestRsaKeyConfiguration.TEST_INSTANT.plusSeconds(60)),
				signedToken(jwtEncoder, USER_ID, jwtProperties.issuer(),
						"wrong-audience", TestRsaKeyConfiguration.TEST_INSTANT.plusSeconds(60)),
				signedToken(jwtEncoder, null, jwtProperties.issuer(),
						jwtProperties.audience(), TestRsaKeyConfiguration.TEST_INSTANT.plusSeconds(60))
		);

		for (String invalidToken : invalidTokens) {
			MvcResult result = mockMvc.perform(get("/api/v1/users/me/consents")
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
					.andExpect(status().isUnauthorized())
					.andReturn();
			assertUnauthorized(result);
			assertThat(result.getResponse().getContentAsString())
					.doesNotContain(invalidToken, "JwtValidationException", "stackTrace");
		}
	}

	@Test
	void nonUuidSubjectIsRejectedSafelyByCurrentUserProvider() throws Exception {
		String token = signedToken(
				jwtEncoder,
				"not-a-uuid",
				jwtProperties.issuer(),
				jwtProperties.audience(),
				TestRsaKeyConfiguration.TEST_INSTANT.plusSeconds(60)
		);

		MvcResult result = mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertUnauthorized(result);
		assertThat(result.getResponse().getContentAsString()).doesNotContain(token, "not-a-uuid");
	}

	private void assertUnauthorized(MvcResult result) throws Exception {
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
		assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(result.getResponse().getCharacterEncoding()).isEqualTo("UTF-8");
		assertThat(result.getResponse().getContentAsString()).isEqualTo(
				"{\"isSuccess\":false,\"code\":\"COMMON_UNAUTHORIZED\","
						+ "\"message\":\"인증이 필요합니다.\",\"result\":null}"
		);
	}

	private String signedToken(
			JwtEncoder selectedEncoder,
			String subject,
			String issuer,
			String audience,
			Instant expiresAt
	) {
		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
				.keyId(jwtProperties.keyId())
				.type("JWT")
				.build();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.audience(List.of(audience))
				.issuedAt(TestRsaKeyConfiguration.TEST_INSTANT.minusSeconds(120))
				.expiresAt(expiresAt)
				.id("security-integration-test-jti")
				.claim("scope", "learning:read learning:write");
		if (subject != null) {
			claims.subject(subject);
		}
		return selectedEncoder.encode(JwtEncoderParameters.from(headers, claims.build()))
				.getTokenValue();
	}

	private JwtEncoder differentKeyEncoder() {
		KeyPair keyPair = generateRsaKeyPair();
		JwtConfiguration configuration = new JwtConfiguration();
		RSAKey rsaKey = configuration.rsaKey(
				(RSAPublicKey) keyPair.getPublic(),
				(RSAPrivateKey) keyPair.getPrivate(),
				new JwtProperties(
						jwtProperties.issuer(),
						jwtProperties.audience(),
						jwtProperties.keyId(),
						Duration.ofMinutes(30),
						"file:not-used-private.pem",
						"file:not-used-public.pem",
						List.of("learning:read", "learning:write")
				)
		);
		return configuration.jwtEncoder(configuration.jwkSource(rsaKey));
	}

	private KeyPair generateRsaKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA is not available in the test runtime.");
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestEndpointConfiguration {

		@Bean
		AuthorityController authorityController() {
			return new AuthorityController();
		}
	}

	@RestController
	static class AuthorityController {

		@GetMapping("/__security-test/authorities")
		Map<String, List<String>> authorities(Authentication authentication) {
			return Map.of(
					"authorities",
					authentication.getAuthorities().stream()
							.map(Object::toString)
							.toList()
			);
		}
	}
}
