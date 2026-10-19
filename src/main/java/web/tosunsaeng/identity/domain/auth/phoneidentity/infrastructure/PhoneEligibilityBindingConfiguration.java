package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PhoneEligibilityBindingProperties.class)
public class PhoneEligibilityBindingConfiguration {

	@Bean
	@ConditionalOnProperty(
			prefix = "app.phone-eligibility-binding",
			name = "enabled",
			havingValue = "true"
	)
	PhoneEligibilityFingerprintHasher phoneEligibilityFingerprintHasher(
			PhoneEligibilityBindingProperties properties
	) {
		return new PhoneEligibilityFingerprintHasher(
				properties.consumerScopeId(),
				properties.keyRegistry()
		);
	}
}
