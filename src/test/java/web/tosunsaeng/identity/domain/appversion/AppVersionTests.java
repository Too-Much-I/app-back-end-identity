package web.tosunsaeng.identity.domain.appversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppVersionTests {
	private MockMvc mvc(String android, String ios) {
		return MockMvcBuilders.standaloneSetup(new AppVersionController(new AppVersionService(android, ios)))
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void returnsConfiguredPlatformsWithoutUpdatePolicy() throws Exception {
		MockMvc mvc = mvc("1.10.0", "2.0.1");
		for (String platform : new String[]{"android", "ios"}) {
			mvc.perform(get("/api/v1/app/version").param("platform", platform))
					.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
					.andExpect(jsonPath("$.isSuccess").value(true))
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.result.platform").value(platform))
					.andExpect(jsonPath("$.result.latestVersion").value(platform.equals("android") ? "1.10.0" : "2.0.1"))
					.andExpect(jsonPath("$.result.length()").value(2));
		}
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"ANDROID", "iOS", "web", " android", "ios ", "android,ios"})
	void invalidOrMissingPlatformIs400(String platform) throws Exception {
		var request = get("/api/v1/app/version");
		if (platform != null) request.param("platform", platform);
		mvc("", "").perform(request).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(header().string("Cache-Control", "no-store"));
	}

	@Test
	void repeatedPlatformsAreRejected() throws Exception {
		mvc("1.0.0", "2.0.0").perform(get("/api/v1/app/version").param("platform", "android", "ios"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unsetPlatformReturns503WithoutDisablingTheOtherPlatform() throws Exception {
		MockMvc mvc = mvc("1.0.0", " ");
		mvc.perform(get("/api/v1/app/version").param("platform", "ios"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("APP_VERSION_UNAVAILABLE"))
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(header().string("Cache-Control", "no-store"));
		mvc.perform(get("/api/v1/app/version").param("platform", "android"))
				.andExpect(status().isOk());
		mvc("", "").perform(get("/api/v1/app/version").param("platform", "android"))
				.andExpect(status().isServiceUnavailable());
	}

	@ParameterizedTest
	@ValueSource(strings = {"latest", "1.2", "v1.2.3", "1.2.3-beta", "01.2.3", "-1.2.3", "1.2.3.4",
			"123456789012345678901234567890123.1.0"})
	void invalidConfigurationIsRejected(String version) {
		assertThatIllegalArgumentException().isThrownBy(() -> new AppVersionService(version, ""));
		assertThatIllegalArgumentException().isThrownBy(() -> new AppVersionService("", version));
	}

	@Test
	void springBindsVersionsAndAllowsAnUnconfiguredDeployment() {
		new ApplicationContextRunner().withUserConfiguration(AppVersionService.class)
				.withPropertyValues("app.version.android-latest-version=1.10.0", "app.version.ios-latest-version=2.0.1")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(AppVersionService.class).getLatest("ios").latestVersion()).isEqualTo("2.0.1");
				});
		new ApplicationContextRunner().withUserConfiguration(AppVersionService.class)
				.run(context -> assertThat(context).hasNotFailed());
		new ApplicationContextRunner().withUserConfiguration(AppVersionService.class)
				.withPropertyValues("app.version.android-latest-version=invalid")
				.run(context -> assertThat(context).hasFailed());
	}
}
