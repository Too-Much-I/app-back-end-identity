package web.tosunsaeng.identity.domain.auth.providerchange;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

class ProviderChangeConfigurationTests {
	ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(ProviderChangeConfiguration.class)
			.withBean(MongoTemplate.class, () -> mock(MongoTemplate.class));
	@Test void defaultsOffDoNotCreateRemoteMutationPort() {
		runner.run(c -> { assertThat(c).hasNotFailed().hasSingleBean(ProviderChangeGuard.class)
				.doesNotHaveBean(ProviderChangeService.class).doesNotHaveBean(FirebaseProviderMutationPort.class)
				.doesNotHaveBean(ProviderLoginRegistrationService.class);
			var p = c.getBean(ProviderChangeProperties.class); assertThat(p.isEnabled()).isFalse();
			assertThat(p.isRelinkEnabled()).isFalse(); assertThat(p.isLinkEnabled()).isFalse(); assertThat(p.isWorkerEnabled()).isFalse(); });
	}
	@Test void loginRegistrationRequiresSessionFenceButNotExplicitLinkCapture() {
		runner.withPropertyValues("app.session-revocation.fence-enabled=true")
				.withBean(web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService.class,
						() -> mock(web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService.class))
				.withBean(web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository.class,
						() -> mock(web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository.class))
				.withBean(web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository.class,
						() -> mock(web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository.class))
				.withBean(web.tosunsaeng.identity.domain.user.domain.repository.UserRepository.class,
						() -> mock(web.tosunsaeng.identity.domain.user.domain.repository.UserRepository.class))
				.withBean(java.time.Clock.class, java.time.Clock::systemUTC)
				.run(c -> assertThat(c).hasNotFailed().hasSingleBean(ProviderLoginRegistrationService.class)
						.doesNotHaveBean(ProviderLinkService.class));
	}
	@Test void commonLinkRequiresFenceAndLegacyFlagCannotExpandScope() {
		runner.withPropertyValues("app.provider-change.link-enabled=true").run(c -> assertThat(c).hasFailed());
		runner.withPropertyValues("app.provider-change.relink-enabled=true").run(c -> assertThat(c).hasFailed());
	}
	@Test void enablingWithoutFenceFailsStartup() {
		runner.withPropertyValues("app.provider-change.enabled=true").run(c -> assertThat(c).hasFailed());
	}
	@Test void fenceWithoutSessionSecurityFailsStartup() {
		runner.withPropertyValues("app.provider-change.fence-enabled=true").run(c -> assertThat(c).hasFailed());
	}
	@Test void invalidLimitsFailRatherThanWeakeningPolicies() {
		var p = new ProviderChangeProperties(); p.setPermitTtl(Duration.ofMinutes(6)); assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
		p.setPermitTtl(Duration.ofMinutes(5)); p.setRecentAuth(Duration.ZERO); assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
		p.setRecentAuth(Duration.ofMinutes(5)); p.setRetention(Duration.ofDays(1)); assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
	}
}
