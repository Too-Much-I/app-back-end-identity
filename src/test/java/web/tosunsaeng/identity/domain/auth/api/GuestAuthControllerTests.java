package web.tosunsaeng.identity.domain.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import web.tosunsaeng.identity.domain.auth.application.EmailAvailabilityService;
import web.tosunsaeng.identity.domain.auth.application.GuestAuthService;
import web.tosunsaeng.identity.domain.auth.application.LoginService;
import web.tosunsaeng.identity.domain.auth.application.LogoutAllService;
import web.tosunsaeng.identity.domain.auth.application.LogoutService;
import web.tosunsaeng.identity.domain.auth.application.SignupService;
import web.tosunsaeng.identity.domain.auth.application.TokenReissueService;
import web.tosunsaeng.identity.domain.auth.dto.request.GuestAuthRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GuestAuthControllerTests {

	private static final String INSTALLATION_ID = "550e8400-e29b-41d4-a716-446655440000";
	private static final String PRIVACY_VERSION = "privacy-v1";
	private static final String TERM_VERSION = "term-v1";
	private static final String QUALITY_REVIEW_VERSION = "quality-review-v1";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmailAvailabilityService emailAvailabilityService;

	@MockitoBean
	private SignupService signupService;

	@MockitoBean
	private GuestAuthService guestAuthService;

	@MockitoBean
	private LoginService loginService;

	@MockitoBean
	private TokenReissueService tokenReissueService;

	@MockitoBean
	private LogoutService logoutService;

	@MockitoBean
	private LogoutAllService logoutAllService;

	@Test
	void guestReturnsExistingTokenShapeAndTrimsInstallationId() throws Exception {
		when(guestAuthService.authenticate(any(GuestAuthRequest.class))).thenReturn(
				new GuestAuthResponse(
						"guest-test-access-value",
						"guest-test-refresh-value",
						"Bearer",
						Duration.ofMinutes(30).toMillis(),
						Duration.ofDays(14).toMillis()
				)
		);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "installationId": "  550e8400-e29b-41d4-a716-446655440000  ",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.result.accessToken").value("guest-test-access-value"))
				.andExpect(jsonPath("$.result.refreshToken").value("guest-test-refresh-value"))
				.andExpect(jsonPath("$.result.grantType").value("Bearer"))
				.andExpect(jsonPath("$.result.accessTokenExpiresIn").value(1_800_000))
				.andExpect(jsonPath("$.result.refreshTokenExpiresIn").value(1_209_600_000))
				.andExpect(jsonPath("$.result.installationId").doesNotExist())
				.andExpect(jsonPath("$.result.installationIdHash").doesNotExist())
				.andReturn();

		ArgumentCaptor<GuestAuthRequest> captor = ArgumentCaptor.forClass(GuestAuthRequest.class);
		verify(guestAuthService).authenticate(captor.capture());
		assertThat(captor.getValue().installationId()).isEqualTo(INSTALLATION_ID);
		assertThat(captor.getValue().isQualityReviewConsented()).isFalse();
		assertThat(captor.getValue().qualityReviewConsentVersion()).isNull();
		assertThat(result.getResponse().getContentAsString()).doesNotContain(INSTALLATION_ID);
	}

	@Test
	void guestAcceptsAndTrimsExplicitQualityReviewConsentFields() throws Exception {
		when(guestAuthService.authenticate(any(GuestAuthRequest.class))).thenReturn(
				new GuestAuthResponse("access", "refresh", "Bearer", 1L, 2L)
		);

		mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(guestJsonWithQuality(true, "  quality-review-v1  ")))
				.andExpect(status().isOk());

		ArgumentCaptor<GuestAuthRequest> captor = ArgumentCaptor.forClass(
				GuestAuthRequest.class
		);
		verify(guestAuthService).authenticate(captor.capture());
		assertThat(captor.getValue().isQualityReviewConsented()).isTrue();
		assertThat(captor.getValue().qualityReviewConsentVersion())
				.isEqualTo(QUALITY_REVIEW_VERSION);
	}

	@ParameterizedTest
	@ValueSource(strings = {" ", "invalid/version", "한글버전"})
	void guestRejectsInvalidQualityReviewVersionFormat(String version) throws Exception {
		mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(guestJsonWithQuality(false, version)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field", hasItem(
						"qualityReviewConsentVersion"
				)));

		verify(guestAuthService, never()).authenticate(any());
	}

	@Test
	void guestRejectsQualityReviewVersionLongerThanOneHundredCharacters() throws Exception {
		mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(guestJsonWithQuality(false, "a".repeat(101))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field", hasItem(
						"qualityReviewConsentVersion"
				)));

		verify(guestAuthService, never()).authenticate(any());
	}

	@Test
	void guestRejectsMissingInstallationIdAndMasksRejectedValue() throws Exception {
		mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field", hasItem("installationId")))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()));

		verify(guestAuthService, never()).authenticate(any());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"   ",
			"not-a-uuid",
			"550e8400-e29b-11d4-a716-446655440000",
			"550e8400-e29b-41d4-7716-446655440000",
			"550e8400-e29b-41d4-a716-446655440000-extra"
	})
	void guestRejectsInvalidInstallationIdWithoutEchoingIt(String invalidInstallationId)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "installationId": "%s",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								""".formatted(invalidInstallationId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field", hasItem("installationId")))
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("installationIdHash");
		if (!invalidInstallationId.isBlank()) {
			assertThat(result.getResponse().getContentAsString())
					.doesNotContain(invalidInstallationId);
		}
		verify(guestAuthService, never()).authenticate(any());
	}

	@Test
	void guestRejectsExcessivelyLongWhitespacePaddedInstallationId() throws Exception {
		String excessiveValue = " ".repeat(65) + INSTALLATION_ID;

		MvcResult result = mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "installationId": "%s",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								""".formatted(excessiveValue)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field", hasItem("installationId")))
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(excessiveValue, INSTALLATION_ID);
		verify(guestAuthService, never()).authenticate(any());
	}

	@Test
	void oldAudioOnlyContractIsRejectedBecauseNewConsentsAreRequired() throws Exception {
		mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"installationId\":\"" + INSTALLATION_ID
								+ "\",\"isAudioConsent\":true}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field", hasItem("isPrivacyConsented")))
				.andExpect(jsonPath("$.result[*].field", hasItem("privacyConsentVersion")))
				.andExpect(jsonPath("$.result[*].field", hasItem("isTermConsented")))
				.andExpect(jsonPath("$.result[*].field", hasItem("termConsentVersion")));

		verify(guestAuthService, never()).authenticate(any());
	}

	@Test
	void guestRejectsFalsePrivacyConsentWithDomainError() throws Exception {
		when(guestAuthService.authenticate(any(GuestAuthRequest.class))).thenThrow(
				new UserException(UserErrorStatus.PRIVACY_CONSENT_REQUIRED)
		);

		mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(guestJson(false, PRIVACY_VERSION, true, TERM_VERSION)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("PRIVACY_CONSENT_REQUIRED"))
				.andExpect(jsonPath("$.message").value("개인정보 처리 동의가 필요합니다."))
				.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void guestRejectsBlankConsentVersionsDuringValidation() throws Exception {
		mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(guestJson(true, "   ", true, "   ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field", hasItem("privacyConsentVersion")))
				.andExpect(jsonPath("$.result[*].field", hasItem("termConsentVersion")));

		verify(guestAuthService, never()).authenticate(any());
	}

	@Test
	void guestReturnsConflictWithoutTokensWhenInstallationAlreadyExists() throws Exception {
		when(guestAuthService.authenticate(any(GuestAuthRequest.class))).thenThrow(
				new AuthException(AuthErrorStatus.GUEST_ALREADY_EXISTS)
		);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validGuestJson()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("GUEST_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.message").value("이미 생성된 Guest 사용자입니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(INSTALLATION_ID, "accessToken", "refreshToken", "installationIdHash");
	}

	@Test
	void unexpectedGuestFailureReturnsSafeErrorWithoutSensitiveRequestOrTokenData()
			throws Exception {
		when(guestAuthService.authenticate(any(GuestAuthRequest.class))).thenThrow(
				new IllegalStateException("test-only internal persistence detail")
		);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validGuestJson()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain(
				INSTALLATION_ID,
				"test-only internal persistence detail",
				"installationIdHash",
				"accessToken",
				"refreshToken"
		);
	}

	@Test
	void nonGuestDuplicateReturnsSafeInternalErrorWithoutIndexOrKeyDetails() throws Exception {
		String internalIndexDetail = "test-only normalized-email-index key detail";
		when(guestAuthService.authenticate(any(GuestAuthRequest.class))).thenThrow(
				new DuplicateKeyException(internalIndexDetail)
		);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/guest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validGuestJson()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain(
				internalIndexDetail,
				INSTALLATION_ID,
				"installationIdHash",
				"keyValue"
		);
	}

	private String validGuestJson() {
		return guestJsonWithQuality(false, QUALITY_REVIEW_VERSION);
	}

	private String guestJsonWithQuality(boolean consented, String version) {
		return """
				{
				  "installationId": "%s",
				  "isPrivacyConsented": true,
				  "privacyConsentVersion": "%s",
				  "isTermConsented": true,
				  "termConsentVersion": "%s",
				  "isQualityReviewConsented": %s,
				  "qualityReviewConsentVersion": "%s"
				}
				""".formatted(
				INSTALLATION_ID,
				PRIVACY_VERSION,
				TERM_VERSION,
				consented,
				version
		);
	}

	private String guestJson(
			boolean privacyConsented,
			String privacyVersion,
			boolean termConsented,
			String termVersion
	) {
		return """
				{
				  "installationId": "%s",
				  "isPrivacyConsented": %s,
				  "privacyConsentVersion": "%s",
				  "isTermConsented": %s,
				  "termConsentVersion": "%s"
				}
				""".formatted(
				INSTALLATION_ID,
				privacyConsented,
				privacyVersion,
				termConsented,
				termVersion
		);
	}
}
