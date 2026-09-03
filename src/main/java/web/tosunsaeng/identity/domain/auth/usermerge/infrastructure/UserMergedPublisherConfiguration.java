package web.tosunsaeng.identity.domain.auth.usermerge.infrastructure;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredentialProvider;
import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedDeliveryPort;
import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedEventMapper;
import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedPublisher;
import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedRetryPolicy;
import web.tosunsaeng.identity.domain.auth.usermerge.repository.UserMergedOutboxRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserMergedPublisherProperties.class)
public class UserMergedPublisherConfiguration {

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(
			prefix = "app.user-merged-publisher",
			name = "enabled",
			havingValue = "true"
	)
	static class EnabledPublisherConfiguration {

		@Bean
		UserMergedDeliveryPort userMergedDeliveryPort(
				UserMergedPublisherProperties properties,
				WorkloadIdentityCredentialProvider credentialProvider,
				Clock clock
		) {
			properties.validate();
			return new JdkUserMergedDeliveryAdapter(
					properties.getEndpoint(),
					properties.getConnectTimeout(),
					properties.getReadTimeout(),
					credentialProvider,
					clock
			);
		}

		@Bean
		UserMergedPublisher userMergedPublisher(
				UserMergedPublisherProperties properties,
				UserMergedOutboxRepository outboxRepository,
				UserMergedDeliveryPort deliveryPort,
				ObjectMapper objectMapper,
				MeterRegistry meterRegistry,
				Clock clock
		) {
			properties.validate();
			return new UserMergedPublisher(
					properties.getLeaseDuration(),
					properties.getMaxAttempts(),
					properties.getPublishedRetention(),
					properties.getDeadLetterReview(),
					outboxRepository,
					new UserMergedEventMapper(objectMapper),
					deliveryPort,
					new UserMergedRetryPolicy(
							() -> ThreadLocalRandom.current().nextDouble()
					),
					meterRegistry,
					clock
			);
		}

		@Bean
		UserMergedPublisherScheduler userMergedPublisherScheduler(
				UserMergedPublisher publisher,
				UserMergedOutboxRepository outboxRepository,
				UserMergedPublisherProperties properties,
				Clock clock
		) {
			return new UserMergedPublisherScheduler(
					publisher,
					outboxRepository,
					properties.getMaxBatchSize(),
					clock
			);
		}
	}
}
