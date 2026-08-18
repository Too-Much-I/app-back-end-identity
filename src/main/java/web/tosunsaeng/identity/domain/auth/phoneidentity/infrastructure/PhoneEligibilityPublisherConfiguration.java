package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingDeliveryPort;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingEventMapper;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingPublisher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityRetryPolicy;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingDeliveryScopeStateRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PhoneEligibilityPublisherProperties.class)
public class PhoneEligibilityPublisherConfiguration {

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(prefix = "app.phone-eligibility-publisher", name = "enabled", havingValue = "true")
	static class EnabledPublisherConfiguration {

		@Bean
		PhoneEligibilityBindingDeliveryPort phoneEligibilityBindingDeliveryPort(
				PhoneEligibilityPublisherProperties publisherProperties,
				PhoneEligibilityBindingProperties bindingProperties,
				WorkloadIdentityCredentialProvider credentialProvider,
				Clock clock
		) {
			publisherProperties.validate(bindingProperties);
			return new JdkPhoneEligibilityBindingDeliveryAdapter(
					publisherProperties.getEndpoint(), publisherProperties.getAudience(),
					publisherProperties.getConnectTimeout(), publisherProperties.getReadTimeout(),
					credentialProvider, clock);
		}

		@Bean
		PhoneEligibilityBindingPublisher phoneEligibilityBindingPublisher(
				PhoneEligibilityPublisherProperties publisherProperties,
				PhoneEligibilityBindingProperties bindingProperties,
				PhoneEligibilityBindingOutboxRepository outboxRepository,
				PhoneEligibilityBindingDeliveryScopeStateRepository scopeStateRepository,
				PhoneEligibilityBindingDeliveryPort deliveryPort,
				ObjectMapper objectMapper, MeterRegistry meterRegistry, Clock clock
		) {
			publisherProperties.validate(bindingProperties);
			return new PhoneEligibilityBindingPublisher(
					bindingProperties.consumerScopeId(), publisherProperties.getLeaseDuration(),
					publisherProperties.getMaxAttempts(), publisherProperties.getPublishedRetention(),
					publisherProperties.getDeadLetterReview(), outboxRepository, scopeStateRepository,
					new PhoneEligibilityBindingEventMapper(objectMapper), deliveryPort,
					new PhoneEligibilityRetryPolicy(() -> ThreadLocalRandom.current().nextDouble()),
					meterRegistry, clock);
		}

		@Bean
		PhoneEligibilityBindingPublisherScheduler phoneEligibilityBindingPublisherScheduler(
				PhoneEligibilityBindingPublisher publisher,
				PhoneEligibilityBindingOutboxRepository outboxRepository,
				PhoneEligibilityPublisherProperties properties,
				Clock clock
		) {
			return new PhoneEligibilityBindingPublisherScheduler(
					publisher, outboxRepository, properties.getMaxBatchSize(), clock);
		}
	}
}
