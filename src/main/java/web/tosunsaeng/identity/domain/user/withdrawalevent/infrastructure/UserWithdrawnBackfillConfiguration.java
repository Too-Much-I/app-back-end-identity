package web.tosunsaeng.identity.domain.user.withdrawalevent.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;

import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnBackfillService;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserWithdrawnBackfillProperties.class)
public class UserWithdrawnBackfillConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(
			UserWithdrawnBackfillConfiguration.class
	);

	@Bean
	@ConditionalOnProperty(
			prefix = "app.user-withdrawn-backfill",
			name = "enabled",
			havingValue = "true"
	)
	ApplicationRunner userWithdrawnBackfillRunner(
			UserWithdrawnBackfillProperties properties,
			MongoOperations mongoOperations,
			UserWithdrawnOutboxRepository repository,
			MeterRegistry meterRegistry
	) {
		properties.validate();
		UserWithdrawnBackfillService service = new UserWithdrawnBackfillService(
				mongoOperations,
				repository
		);
		return arguments -> {
			UserWithdrawnBackfillService.Result result = service.run(
					properties.getLowerBound(),
					properties.getUpperBound(),
					properties.getBatchLimit(),
					properties.isDryRun()
			);
			meterRegistry.counter(
					"identity.user_withdrawn.backfill",
					"dryRun", Boolean.toString(properties.isDryRun()),
					"outcome", "COMPLETED"
			).increment();
			LOGGER.info(
					"UserWithdrawn backfill batch completed. dryRun={}, selected={}, missing={}, created={}, conflicts={}",
					properties.isDryRun(),
					result.selected(),
					result.missing(),
					result.created(),
					result.concurrentConflicts()
			);
		};
	}
}
