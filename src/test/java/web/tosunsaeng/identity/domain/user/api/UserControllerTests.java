package web.tosunsaeng.identity.domain.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.application.UserProfileService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserProfileService userProfileService;

	@Test
	void getMeDelegatesToServiceAndWrapsDedicatedProfileDto() throws Exception {
		UserProfileResponse profile = new UserProfileResponse(
				USER_ID,
				"user@example.com",
				"토스마스터",
				UserProvider.LOCAL,
				true,
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
				.andExpect(jsonPath("$.result.isAudioConsent").value(true))
				.andExpect(jsonPath("$.result.createdAt").value("2026-07-27T05:06:07Z"))
				.andExpect(jsonPath("$.result.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.result.normalizedEmail").doesNotExist())
				.andExpect(jsonPath("$.result.refreshSession").doesNotExist())
				.andExpect(jsonPath("$.result.streakDays").doesNotExist());

		verify(userProfileService).getCurrentUserProfile();
	}

	@Test
	void controllerDependsOnServiceRatherThanSecurityContextOrRepositories() {
		assertThat(Arrays.stream(UserController.class.getDeclaredFields()).map(Field::getType))
				.containsExactly(UserProfileService.class)
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
				Instant.parse("2026-07-30T08:00:00Z")
		);
		when(userProfileService.getCurrentUserProfile()).thenReturn(profile);

		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.userId").value(USER_ID))
				.andExpect(jsonPath("$.result.email").value(nullValue()))
				.andExpect(jsonPath("$.result.nickname").value("게스트"))
				.andExpect(jsonPath("$.result.provider").value("GUEST"))
				.andExpect(jsonPath("$.result.isAudioConsent").value(true))
				.andExpect(jsonPath("$.result.installationId").doesNotExist())
				.andExpect(jsonPath("$.result.guestInstallationIdHash").doesNotExist())
				.andExpect(jsonPath("$.result.accessToken").doesNotExist())
				.andExpect(jsonPath("$.result.refreshToken").doesNotExist());
	}
}
