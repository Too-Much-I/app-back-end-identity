package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneIdentityService;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneIdentityTransactionService;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKeyRegistry;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;

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
				PhoneFingerprintAliasRepository aliasRepository
		) {
			return new PhoneIdentityTransactionService(
					phoneIdentityRepository,
					aliasRepository
			);
		}

		@Bean
		PhoneIdentityService phoneIdentityService(
				PhoneNumberNormalizer phoneNumberNormalizer,
				PhoneFingerprintHasher fingerprintHasher,
				PhoneIdentityTransactionService transactionService,
				Clock clock
		) {
			return new PhoneIdentityService(
					phoneNumberNormalizer,
					fingerprintHasher,
					transactionService,
					clock
			);
		}
	}
}
