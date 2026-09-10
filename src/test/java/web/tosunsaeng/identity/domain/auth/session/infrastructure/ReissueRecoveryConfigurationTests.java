package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.*;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.MongoTransactionManager;
import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.repository.*;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

class ReissueRecoveryConfigurationTests {
	@TempDir Path directory;
	ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(ReissueRecoveryConfiguration.class);
	@Test void defaultOffDoesNotLoadKeyOrRequireInfrastructure() {
		runner.run(c -> { assertThat(c).hasNotFailed().doesNotHaveBean(ReissueRecoveryService.class).doesNotHaveBean(ReissueEncryptionKeyProvider.class);
			assertThat(c.getBean(ReissueRecoveryProperties.class).isEnabled()).isFalse(); });
	}
	@Test void enabledWithoutFenceFailsStartup() { runner.withPropertyValues("app.reissue-recovery.enabled=true").run(c -> assertThat(c).hasFailed()); }
	@Test void enabledWithoutKeysFailsStartup() {
		infrastructure().withPropertyValues("app.reissue-recovery.enabled=true", "app.reissue-recovery.environment=test")
				.run(c -> assertThat(c).hasFailed());
	}
	@Test void validMountEnablesOnlyLocalRecoveryService() throws Exception {
		Path mount = directory.resolve("test.properties"); Files.writeString(mount, "test-v1=" + Base64.getEncoder().encodeToString(new byte[32]));
		infrastructure().withPropertyValues("app.reissue-recovery.enabled=true", "app.reissue-recovery.environment=test",
				"app.reissue-recovery.encryption-active-key-id=test-v1", "app.reissue-recovery.encryption-keyring-location=" + mount)
				.run(c -> assertThat(c).hasNotFailed().hasSingleBean(ReissueRecoveryService.class).hasSingleBean(ReissueResponseCipher.class));
	}
	@ParameterizedTest @ValueSource(strings = {"PT0S", "PT-1S", "PT121S"})
	void recoveryCannotExceedApprovedWindow(String value) {
		var p = new ReissueRecoveryProperties(); p.setEnabled(true); p.setEnvironment("test"); p.setWindow(Duration.parse(value));
		assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
	}
	@Test void invalidRetrySizeOrEnvironmentFails() {
		var p = new ReissueRecoveryProperties(); p.setEnabled(true); p.setEnvironment("test"); p.setConcurrencyAttempts(4);
		assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
		p.setConcurrencyAttempts(3); p.setResponseMaxBytes(16385); assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
		p.setResponseMaxBytes(16384); p.setEnvironment(""); assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
	}
	ApplicationContextRunner infrastructure() {
		return runner.withBean(SessionSecurityService.class, () -> mock(SessionSecurityService.class))
				.withBean(RefreshSessionRepository.class, () -> mock(RefreshSessionRepository.class))
				.withBean(RefreshReissueResponseRepository.class, () -> mock(RefreshReissueResponseRepository.class))
				.withBean(UserRepository.class, () -> mock(UserRepository.class))
				.withBean(RefreshTokenHasher.class, RefreshTokenHasher::new)
				.withBean(RefreshSessionIssuer.class, () -> mock(RefreshSessionIssuer.class))
				.withBean(AccessTokenIssuer.class, () -> mock(AccessTokenIssuer.class))
				.withBean(AuthResponseConverter.class, AuthResponseConverter::new)
				.withBean(MongoTransactionManager.class, () -> {
					var manager = mock(MongoTransactionManager.class);
					when(manager.getDatabaseFactory()).thenReturn(mock(org.springframework.data.mongodb.MongoDatabaseFactory.class));
					return manager;
				})
				.withBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
				.withBean(Clock.class, Clock::systemUTC).withBean(MeterRegistry.class, SimpleMeterRegistry::new);
	}
}
