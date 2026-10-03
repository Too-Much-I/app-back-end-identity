package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.federation.repository.*;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.*;
import web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard;
import web.tosunsaeng.identity.domain.user.domain.repository.*;

class AccountRecoveryConfigurationTests {
	ApplicationContextRunner runner() {
		var firebase = mock(FirebaseAuthProperties.class);
		when(firebase.enabled()).thenReturn(true); when(firebase.phoneEnabled()).thenReturn(true);
		return new ApplicationContextRunner().withUserConfiguration(AccountRecoveryConfiguration.class)
				.withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
				.withBean(MongoTransactionManager.class, () -> mock(MongoTransactionManager.class))
				.withBean(FirebaseAuthProperties.class, () -> firebase)
				.withBean(FirebaseAuthenticationVerifier.class, () -> mock(FirebaseAuthenticationVerifier.class))
				.withBean(PhoneFingerprintHasher.class, () -> mock(PhoneFingerprintHasher.class))
				.withBean(PhoneFingerprintAliasRepository.class, () -> mock(PhoneFingerprintAliasRepository.class))
				.withBean(PhoneIdentityRepository.class, () -> mock(PhoneIdentityRepository.class))
				.withBean(UserRepository.class, () -> mock(UserRepository.class))
				.withBean(SocialIdentityRepository.class, () -> mock(SocialIdentityRepository.class))
				.withBean(FirebaseIdentityRepository.class, () -> mock(FirebaseIdentityRepository.class))
				.withBean(UserWithdrawalLifecycleRepository.class, () -> mock(UserWithdrawalLifecycleRepository.class))
				.withBean(ProviderChangeGuard.class, () -> mock(ProviderChangeGuard.class))
				.withBean(Clock.class, Clock::systemUTC);
	}
	@Test void defaultOffDoesNotCreateServiceOrRequireKeys() {
		runner().run(context -> { assertThat(context).hasNotFailed(); assertThat(context).doesNotHaveBean(AccountRecoveryService.class); });
	}
	@Test void onCreatesServiceUsingExistingFirebaseAndPhoneInfrastructure() {
		runner().withPropertyValues("app.account-recovery.enabled=true", "app.account-recovery.key-ring=" + AccountRecoveryServiceTests.RING)
				.run(context -> { assertThat(context).hasNotFailed(); assertThat(context).hasSingleBean(AccountRecoveryService.class); });
	}
	@Test void onWithoutIndependentKeyFailsClosed() {
		runner().withPropertyValues("app.account-recovery.enabled=true").run(context -> assertThat(context).hasFailed());
	}
}
