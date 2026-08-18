package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;

import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneIdentityService;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneIdentityTransactionService;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKeyRegistry;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PhoneFingerprintProperties.class)
public class PhoneIdentityConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(
			prefix = "app.phone-identity.fingerprint",
			name = "enabled",
			havingValue = "true"
	)
	static class EnabledPhoneIdentityConfiguration {

		@Bean
		PhoneFingerprintKeyRegistry phoneFingerprintKeyRegistry(
				PhoneFingerprintProperties properties
		) {
			return properties.keyRegistry();
		}

		@Bean
		PhoneNumberNormalizer phoneNumberNormalizer() {
			return new PhoneNumberNormalizer();
		}

		@Bean
		PhoneFingerprintHasher phoneFingerprintHasher(
				PhoneFingerprintKeyRegistry keyRegistry
		) {
			return new PhoneFingerprintHasher(keyRegistry);
		}

		@Bean
		PhoneIdentityTransactionService phoneIdentityTransactionService(
				PhoneIdentityRepository phoneIdentityRepository,
				PhoneFingerprintAliasRepository aliasRepository,
				ObjectProvider<PhoneEligibilityBindingRevisionRepository> revisionRepository,
				ObjectProvider<PhoneEligibilityBindingOutboxRepository> outboxRepository
		) {
			return new PhoneIdentityTransactionService(
					phoneIdentityRepository,
					aliasRepository,
					revisionRepository.getIfAvailable(),
					outboxRepository.getIfAvailable()
			);
		}

		@Bean
		PhoneIdentityService phoneIdentityService(
				PhoneNumberNormalizer phoneNumberNormalizer,
				PhoneFingerprintHasher fingerprintHasher,
				ObjectProvider<PhoneEligibilityFingerprintHasher> eligibilityFingerprintHasher,
				PhoneIdentityTransactionService transactionService,
				Clock clock
		) {
			return new PhoneIdentityService(
					phoneNumberNormalizer,
					fingerprintHasher,
					eligibilityFingerprintHasher.getIfAvailable(),
					transactionService,
					clock
			);
		}
	}
}
