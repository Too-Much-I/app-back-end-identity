package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

class ConsentPolicyTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(
					PropertySourcesPlaceholderConfigurer.class,
					PropertySourcesPlaceholderConfigurer::new
			)
			.withUserConfiguration(ConsentPolicy.class);

	@Test
	void startsWithNonBlankConfiguredVersions() {
		contextRunner.withPropertyValues(
				"app.consent.privacy-version=privacy-v1",
				"app.consent.term-version=term-v1"
		).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(ConsentPolicy.class);
		});
	}

	@Test
	void failsFastWhenPrivacyVersionIsMissing() {
		contextRunner.withPropertyValues(
				"app.consent.term-version=term-v1"
		).run(context -> {
			assertThat(context).hasFailed();
			assertThat(rootCause(context.getStartupFailure()).getMessage())
					.contains("app.consent.privacy-version");
		});
	}

	@Test
	void failsFastWhenTermVersionIsMissing() {
		contextRunner.withPropertyValues(
				"app.consent.privacy-version=privacy-v1"
		).run(context -> {
			assertThat(context).hasFailed();
			assertThat(rootCause(context.getStartupFailure()).getMessage())
					.contains("app.consent.term-version");
		});
	}

	@Test
	void failsFastWhenEitherVersionIsBlank() {
		contextRunner.withPropertyValues(
				"app.consent.privacy-version=",
				"app.consent.term-version=term-v1"
		).run(context -> {
			assertThat(context).hasFailed();
			assertThat(rootCause(context.getStartupFailure()).getMessage())
					.contains("privacyConsentVersion must not be blank");
		});

		contextRunner.withPropertyValues(
				"app.consent.privacy-version=privacy-v1",
				"app.consent.term-version= "
		).run(context -> {
			assertThat(context).hasFailed();
			assertThat(rootCause(context.getStartupFailure()).getMessage())
					.contains("termConsentVersion must not be blank");
		});
	}

	private Throwable rootCause(Throwable throwable) {
		Throwable cause = throwable;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause;
	}
}
