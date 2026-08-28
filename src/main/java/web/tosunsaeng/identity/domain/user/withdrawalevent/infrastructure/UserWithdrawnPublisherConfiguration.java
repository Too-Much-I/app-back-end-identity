package web.tosunsaeng.identity.domain.user.withdrawalevent.infrastructure;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredentialProvider;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnOutboxStatus;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnDeliveryPort;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnEventMapper;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnPublisher;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnReplayService;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnRetryPolicy;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserWithdrawnPublisherProperties.class)
public class UserWithdrawnPublisherConfiguration {

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(
			prefix = "app.user-withdrawn-publisher",
			name = "enabled",
			havingValue = "true"
	)
	static class EnabledPublisherConfiguration {

		@Bean
		UserWithdrawnDeliveryPort userWithdrawnDeliveryPort(
				UserWithdrawnPublisherProperties properties,
				WorkloadIdentityCredentialProvider credentialProvider,
				Clock clock
		) {
			properties.validate();
			return new JdkUserWithdrawnDeliveryAdapter(
					properties.getEndpoint(),
					properties.getAudience(),
					properties.getConnectTimeout(),
					properties.getReadTimeout(),
					credentialProvider,
					clock
			);
		}

		@Bean
		UserWithdrawnPublisher userWithdrawnPublisher(
				UserWithdrawnPublisherProperties properties,
				UserWithdrawnOutboxRepository repository,
				UserWithdrawnDeliveryPort deliveryPort,
				ObjectMapper objectMapper,
				MeterRegistry meterRegistry,
				Clock clock
		) {
			properties.validate();
			return new UserWithdrawnPublisher(
					properties.getLeaseDuration(),
					properties.getMaxAttempts(),
					properties.getPublishedRetention(),
					properties.getDeadLetterReview(),
					repository,
					new UserWithdrawnEventMapper(objectMapper),
					deliveryPort,
					new UserWithdrawnRetryPolicy(
							() -> ThreadLocalRandom.current().nextDouble()
					),
					meterRegistry,
					clock
			);
		}

		@Bean
		Object userWithdrawnPublisherGauges(
				UserWithdrawnOutboxRepository repository,
				MeterRegistry meterRegistry,
				Clock clock
		) {
			Gauge.builder(
					"identity.user_withdrawn.backlog",
					repository,
					repo -> repo.countByStatus(UserWithdrawnOutboxStatus.PENDING)
			).register(meterRegistry);
			Gauge.builder(
					"identity.user_withdrawn.dead_letter",
					repository,
					repo -> repo.countByStatus(UserWithdrawnOutboxStatus.DEAD_LETTER)
			).register(meterRegistry);
			Gauge.builder(
					"identity.user_withdrawn.oldest_age_seconds",
					repository,
					repo -> repo.findFirstByStatusOrderByWithdrawnAtAsc(
							UserWithdrawnOutboxStatus.PENDING
					).map(event -> Math.max(
							0L,
							java.time.Duration.between(
									event.getWithdrawnAt(),
									clock.instant()
							).toSeconds()
					)).orElse(0L)
			).register(meterRegistry);
			return new Object();
		}

		@Bean
		UserWithdrawnReplayService userWithdrawnReplayService(
				UserWithdrawnOutboxRepository repository,
				Clock clock
		) {
			return new UserWithdrawnReplayService(repository, clock);
		}

		@Bean
		UserWithdrawnPublisherScheduler userWithdrawnPublisherScheduler(
				UserWithdrawnPublisher publisher,
				UserWithdrawnOutboxRepository repository,
				UserWithdrawnPublisherProperties properties,
				Clock clock
		) {
			return new UserWithdrawnPublisherScheduler(
					publisher,
					repository,
					properties.getMaxBatchSize(),
					clock
			);
		}
	}
}
