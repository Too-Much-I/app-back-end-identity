package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class FirebaseAuthPropertiesTests {

	@Test
	void disabledConfigurationUsesSafePolicyDefaultsWithoutProject() {
		FirebaseAuthProperties properties = properties(false, null);

		assertThat(properties.enabled()).isFalse();
		assertThat(properties.projectId()).isNull();
		assertThat(properties.loginMaxAuthenticationAge()).isEqualTo(Duration.ofMinutes(15));
		assertThat(properties.highRiskMaxAuthenticationAge()).isEqualTo(Duration.ofMinutes(5));
		assertThat(properties.enrollmentTtl()).isEqualTo(Duration.ofMinutes(10));
		assertThat(properties.toString())
				.contains("projectId=[REDACTED]")
				.doesNotContain("test-project");
	}

	@Test
	void enabledConfigurationRequiresProjectId() {
		assertThatThrownBy(() -> properties(true, " "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("project-id is required");
	}

	@Test
	void kakaoProviderMustUseOidcNamespaceWhenEnabled() {
		assertThatThrownBy(() -> new FirebaseAuthProperties(
				true,
				"test-project",
				null,
				false,
				false,
				true,
				false,
				"kakao",
				null,
				null,
				null,
				null,
				null,
				null,
				null
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Kakao Firebase provider ID must start with oidc.");
	}

	private FirebaseAuthProperties properties(boolean enabled, String projectId) {
		return new FirebaseAuthProperties(
				enabled,
				projectId,
				null,
				false,
				false,
				false,
				false,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}
}
