package web.tosunsaeng.identity.domain.user.infrastructure;

import java.time.Clock;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableScheduling;

import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.PhoneRejoinLineageRepository;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalIdentityReleaseTransactionService;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalIdentityReleaseWorker;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserWithdrawalIdentityReleaseProperties.class)
public class UserWithdrawalIdentityReleaseConfiguration {

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(
			prefix = "app.firebase-withdrawal-identity-release",
			name = "enabled",
			havingValue = "true"
	)
	static class EnabledIdentityReleaseConfiguration {

		@Bean
		UserWithdrawalIdentityReleaseTransactionService identityReleaseTransactionService(
				UserWithdrawalLifecycleRepository lifecycleRepository,
				UserRepository userRepository,
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				PhoneIdentityRepository phoneIdentityRepository,
				PhoneFingerprintAliasRepository aliasRepository,
				PhoneEligibilityBindingRevisionRepository bindingRevisionRepository,
				PhoneEligibilityBindingOutboxRepository bindingOutboxRepository,
				@Nullable PhoneRejoinLineageRepository lineageRepository,
				@Nullable OwnerEventProperties ownerEventProperties,
				UserWithdrawalIdentityReleaseProperties properties
		) {
			properties.validate();
			return new UserWithdrawalIdentityReleaseTransactionService(
					lifecycleRepository,
					userRepository,
					firebaseIdentityRepository,
					socialIdentityRepository,
					phoneIdentityRepository,
					aliasRepository,
					bindingRevisionRepository,
					bindingOutboxRepository,
					lineageRepository,
					ownerEventProperties
			);
		}

		@Bean
		UserWithdrawalIdentityReleaseWorker identityReleaseWorker(
				UserWithdrawalLifecycleRepository lifecycleRepository,
				UserWithdrawalIdentityReleaseTransactionService transactionService,
				MeterRegistry meterRegistry,
				Clock clock
		) {
			return new UserWithdrawalIdentityReleaseWorker(
					lifecycleRepository,
					transactionService,
					meterRegistry,
					clock
			);
		}

		@Bean
		UserWithdrawalIdentityReleaseScheduler identityReleaseScheduler(
				UserWithdrawalIdentityReleaseWorker worker,
				UserWithdrawalIdentityReleaseProperties properties
		) {
			return new UserWithdrawalIdentityReleaseScheduler(
					worker,
					properties.getMaxBatchSize()
			);
		}
	}
}
