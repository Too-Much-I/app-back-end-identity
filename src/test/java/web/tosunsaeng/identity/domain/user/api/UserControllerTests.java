package web.tosunsaeng.identity.domain.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.domain.user.application.UserConsentService;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.dto.response.ConsentPolicyStatusResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentStatusResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.application.UserProfileService;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant CONSENTED_AT = Instant.parse("2026-08-05T00:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserProfileService userProfileService;

	@MockitoBean
	private UserConsentService userConsentService;

	@Test
	void getMeDelegatesToServiceAndWrapsDedicatedProfileDto() throws Exception {
		UserProfileResponse profile = new UserProfileResponse(
				USER_ID,
				"user@example.com",
				"토스마스터",
				UserProvider.LOCAL,
				true,
				"privacy-v1",
				CONSENTED_AT,
				true,
				"term-v1",
				CONSENTED_AT,
				Instant.parse("2026-07-27T05:06:07Z")
		);
		when(userProfileService.getCurrentUserProfile()).thenReturn(profile);

		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.userId").value(USER_ID))
				.andExpect(jsonPath("$.result.email").value("user@example.com"))
				.andExpect(jsonPath("$.result.nickname").value("토스마스터"))
				.andExpect(jsonPath("$.result.provider").value("LOCAL"))
				.andExpect(jsonPath("$.result.privacyConsented").value(true))
				.andExpect(jsonPath("$.result.privacyConsentVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.privacyConsentedAt").value("2026-08-05T00:00:00Z"))
				.andExpect(jsonPath("$.result.termConsented").value(true))
				.andExpect(jsonPath("$.result.termConsentVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.termConsentedAt").value("2026-08-05T00:00:00Z"))
				.andExpect(jsonPath("$.result.createdAt").value("2026-07-27T05:06:07Z"))
				.andExpect(jsonPath("$.result.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.result.normalizedEmail").doesNotExist())
				.andExpect(jsonPath("$.result.refreshSession").doesNotExist())
				.andExpect(jsonPath("$.result.streakDays").doesNotExist());

		verify(userProfileService).getCurrentUserProfile();
	}

	@Test
	void getConsentsReturnsCurrentVersionsAndConsentRequirements() throws Exception {
		UserConsentStatusResponse response = new UserConsentStatusResponse(
				new ConsentPolicyStatusResponse(
						"privacy-v2",
						true,
						"privacy-v1",
						CONSENTED_AT,
						true
				),
				new ConsentPolicyStatusResponse(
						"term-v1",
						true,
						"term-v1",
						CONSENTED_AT,
						false
				)
		);
		when(userConsentService.getCurrentConsentStatus()).thenReturn(response);

		mockMvc.perform(get("/api/v1/users/me/consents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.result.privacy.currentVersion").value("privacy-v2"))
				.andExpect(jsonPath("$.result.privacy.consented").value(true))
				.andExpect(jsonPath("$.result.privacy.consentedVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.privacy.consentedAt")
						.value("2026-08-05T00:00:00Z"))
				.andExpect(jsonPath("$.result.privacy.requiresConsent").value(true))
				.andExpect(jsonPath("$.result.terms.currentVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.terms.consented").value(true))
				.andExpect(jsonPath("$.result.terms.consentedVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.terms.consentedAt")
						.value("2026-08-05T00:00:00Z"))
				.andExpect(jsonPath("$.result.terms.requiresConsent").value(false))
				.andExpect(jsonPath("$.result.userId").doesNotExist())
				.andExpect(jsonPath("$.result.installationId").doesNotExist());

		verify(userConsentService).getCurrentConsentStatus();
	}

	@Test
	void getConsentsReturnsExistingUserNotFoundErrorShape() throws Exception {
		when(userConsentService.getCurrentConsentStatus())
				.thenThrow(new UserException(UserErrorStatus.USER_NOT_FOUND));

		mockMvc.perform(get("/api/v1/users/me/consents"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void controllerDependsOnServiceRatherThanSecurityContextOrRepositories() {
		Class<?>[] dependencyTypes = Arrays.stream(UserController.class.getDeclaredFields())
				.map(Field::getType)
				.toArray(Class<?>[]::new);

		assertThat(dependencyTypes)
				.containsExactlyInAnyOrder(UserProfileService.class, UserConsentService.class)
				.doesNotContain(UserRepository.class);
	}

	@Test
	void getMeAllowsGuestProfileWithNullEmailAndNoInstallationData() throws Exception {
		UserProfileResponse profile = new UserProfileResponse(
				USER_ID,
				null,
				"게스트",
				UserProvider.GUEST,
				true,
				"privacy-v1",
				CONSENTED_AT,
				true,
				"term-v1",
				CONSENTED_AT,
				Instant.parse("2026-07-30T08:00:00Z")
		);
		when(userProfileService.getCurrentUserProfile()).thenReturn(profile);

		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.userId").value(USER_ID))
				.andExpect(jsonPath("$.result.email").value(nullValue()))
				.andExpect(jsonPath("$.result.nickname").value("게스트"))
				.andExpect(jsonPath("$.result.provider").value("GUEST"))
				.andExpect(jsonPath("$.result.privacyConsented").value(true))
				.andExpect(jsonPath("$.result.termConsented").value(true))
				.andExpect(jsonPath("$.result.installationId").doesNotExist())
				.andExpect(jsonPath("$.result.guestInstallationIdHash").doesNotExist())
				.andExpect(jsonPath("$.result.accessToken").doesNotExist())
				.andExpect(jsonPath("$.result.refreshToken").doesNotExist());
	}

	@Test
	void updateConsentsDelegatesAndReturnsStoredVersionsAndServerTimes() throws Exception {
		UserConsentResponse response = new UserConsentResponse(
				true,
				"privacy-v1",
				CONSENTED_AT,
				true,
				"term-v1",
				CONSENTED_AT
		);
		when(userConsentService.updateConsents(org.mockito.ArgumentMatchers.any()))
				.thenReturn(response);

		mockMvc.perform(put("/api/v1/users/me/consents")
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
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.privacyConsented").value(true))
				.andExpect(jsonPath("$.result.privacyConsentVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.privacyConsentedAt").value("2026-08-05T00:00:00Z"))
				.andExpect(jsonPath("$.result.termConsented").value(true))
				.andExpect(jsonPath("$.result.termConsentVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.termConsentedAt").value("2026-08-05T00:00:00Z"));

		verify(userConsentService).updateConsents(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void updateConsentsRejectsMissingRequiredConsentFields() throws Exception {
		mockMvc.perform(put("/api/v1/users/me/consents")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}
}
