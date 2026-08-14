package web.tosunsaeng.identity.domain.auth.infrastructure.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import web.tosunsaeng.identity.domain.auth.application.phone.PhoneIdentityService;
import web.tosunsaeng.identity.domain.auth.domain.phone.PhoneFingerprintKeyRegistry;
import web.tosunsaeng.identity.domain.auth.domain.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.domain.repository.PhoneIdentityRepository;

class PhoneIdentityConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					PhoneIdentityConfiguration.class,
					TestDependencies.class
			);

	@Test
	void disabledByDefaultDoesNotCreateFingerprintOrServiceBeans() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(PhoneFingerprintKeyRegistry.class);
			assertThat(context).doesNotHaveBean(PhoneIdentityService.class);
		});
	}

	@Test
	void enabledConfigurationFailsClosedWithoutValidKeyRing() {
		contextRunner
				.withPropertyValues("app.phone-identity.fingerprint.enabled=true")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void enabledConfigurationCreatesInternalFoundationWithExternalKeyRing() {
		String keyRing = "v1,LOOKUP_ONLY," + encodedKey(1)
				+ ";v2,ACTIVE_WRITE," + encodedKey(2);
		contextRunner
				.withPropertyValues(
						"app.phone-identity.fingerprint.enabled=true",
						"app.phone-identity.fingerprint.key-ring=" + keyRing
				)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(PhoneFingerprintKeyRegistry.class);
					assertThat(context).hasSingleBean(PhoneIdentityService.class);
					assertThat(context.getBean(PhoneFingerprintKeyRegistry.class)
							.activeWriteKey().version()).isEqualTo("v2");
				});
	}

	private String encodedKey(int fill) {
		byte[] material = new byte[32];
		Arrays.fill(material, (byte) fill);
		return Base64.getEncoder().encodeToString(material);
	}

	@Configuration(proxyBeanMethods = false)
	static class TestDependencies {

		@Bean
		Clock clock() {
			return Clock.systemUTC();
		}

		@Bean
		PhoneIdentityRepository phoneIdentityRepository() {
			return mock(PhoneIdentityRepository.class);
		}

		@Bean
		PhoneFingerprintAliasRepository phoneFingerprintAliasRepository() {
			return mock(PhoneFingerprintAliasRepository.class);
		}
	}
}
