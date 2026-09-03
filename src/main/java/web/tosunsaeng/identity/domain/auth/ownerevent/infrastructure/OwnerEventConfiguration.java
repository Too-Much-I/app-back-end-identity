package web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventCaptureService;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventDeliveryPort;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventPublishTransactionService;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventOperationsService;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventPublisher;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventRetryPolicy;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventWireMapper;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.PhoneRejoinLineageResolver;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.PhoneRejoinLineageRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredentialProvider;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OwnerEventProperties.class)
public class OwnerEventConfiguration {

	@Bean
	@ConditionalOnExpression("'${app.owner-event.user-merged-capture-enabled:false}' == 'true' || '${app.owner-event.trial-rebind-capture-enabled:false}' == 'true'")
	OwnerEventCaptureService ownerEventCaptureService(
			OwnerEventCoreRepository coreRepository,
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository
	) {
		return new OwnerEventCaptureService(coreRepository, deliveryRepository, stateRepository);
	}

	@Bean
	@ConditionalOnProperty(prefix = "app.owner-event", name = "trial-rebind-capture-enabled", havingValue = "true")
	PhoneRejoinLineageResolver phoneRejoinLineageResolver(
			OwnerEventProperties properties,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneRejoinLineageRepository lineageRepository,
			PhoneEligibilityBindingRevisionRepository revisionRepository,
			UserRepository userRepository,
			UserWithdrawalLifecycleRepository lifecycleRepository,
			OwnerEventCaptureService captureService
	) {
		return new PhoneRejoinLineageResolver(properties, aliasRepository, lineageRepository,
				revisionRepository, userRepository, lifecycleRepository, captureService);
	}

	@Bean
	@ConditionalOnExpression("'${app.owner-event.billing-user-merged-publisher-enabled:false}' == 'true' || '${app.owner-event.learning-core-user-merged-publisher-enabled:false}' == 'true' || '${app.owner-event.billing-trial-rebind-publisher-enabled:false}' == 'true'")
	OwnerEventPublishTransactionService ownerEventPublishTransactionService(
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository,
			OwnerEventCoreRepository coreRepository,
			PhoneRejoinLineageRepository lineageRepository
	) {
		return new OwnerEventPublishTransactionService(
				deliveryRepository, stateRepository, coreRepository, lineageRepository);
	}

	@Bean
	@ConditionalOnExpression("'${app.owner-event.billing-user-merged-publisher-enabled:false}' == 'true' || '${app.owner-event.learning-core-user-merged-publisher-enabled:false}' == 'true' || '${app.owner-event.billing-trial-rebind-publisher-enabled:false}' == 'true'")
	OwnerEventOperationsService ownerEventOperationsService(
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository,
			OwnerEventPublishTransactionService transactionService,
			Clock clock
	) {
		return new OwnerEventOperationsService(
				deliveryRepository, stateRepository, transactionService, clock);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnExpression("'${app.owner-event.billing-user-merged-publisher-enabled:false}' == 'true' || '${app.owner-event.billing-trial-rebind-publisher-enabled:false}' == 'true'")
	static class BillingPublisherConfiguration {
		@Bean("ownerEventAwsCredentialsProvider")
		AwsCredentialsProvider ownerEventAwsCredentialsProvider() {
			return DefaultCredentialsProvider.create();
		}

		@Bean("billingOwnerEventDeliveryPort")
		OwnerEventDeliveryPort billingOwnerEventDeliveryPort(
				OwnerEventProperties properties,
				@Qualifier("ownerEventAwsCredentialsProvider") AwsCredentialsProvider credentialsProvider
		) {
			properties.validateBilling();
			return new BillingOwnerEventDeliveryAdapter(new web.tosunsaeng.identity.global.workload.BillingSigV4JsonTransport(
					properties.getBillingBaseUrl(), properties.getBillingRegion(),
					properties.getConnectTimeout(), properties.getReadTimeout(), credentialsProvider));
		}

		@Bean("billingOwnerEventPublisher")
		OwnerEventPublisher billingOwnerEventPublisher(
				OwnerEventProperties properties,
				OwnerEventCoreRepository coreRepository,
				OwnerEventDeliveryRepository deliveryRepository,
				OwnerEventConsumerStateRepository stateRepository,
				OwnerEventPublishTransactionService transactionService,
				@Qualifier("billingOwnerEventDeliveryPort") OwnerEventDeliveryPort deliveryPort,
				ObjectMapper objectMapper, MeterRegistry meterRegistry, Clock clock
		) {
			properties.validateBilling();
			return publisher(OwnerEventConsumer.BILLING, properties, coreRepository,
					deliveryRepository, stateRepository, transactionService,
					deliveryPort, objectMapper, meterRegistry, clock);
		}

		@Bean("billingOwnerEventPublisherScheduler")
		OwnerEventPublisherScheduler billingOwnerEventPublisherScheduler(
				@Qualifier("billingOwnerEventPublisher") OwnerEventPublisher publisher,
				OwnerEventDeliveryRepository deliveryRepository,
				OwnerEventCoreRepository coreRepository,
				OwnerEventProperties properties, Clock clock
		) {
			return new OwnerEventPublisherScheduler(
					publisher, deliveryRepository, coreRepository,
					properties.getMaxBatchSize(), clock);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnExpression("'${app.owner-event.learning-core-user-merged-publisher-enabled:false}' == 'true'")
	static class LearningCorePublisherConfiguration {
		@Bean("learningCoreOwnerEventDeliveryPort")
		OwnerEventDeliveryPort learningCoreOwnerEventDeliveryPort(
				OwnerEventProperties properties,
				WorkloadIdentityCredentialProvider credentialProvider,
				Clock clock
		) {
			properties.validateLearningCore();
			return new LearningCoreOwnerEventDeliveryAdapter(
					properties.getLearningCoreEndpoint(),
					properties.getConnectTimeout(), properties.getReadTimeout(),
					credentialProvider, clock);
		}

		@Bean("learningCoreOwnerEventPublisher")
		OwnerEventPublisher learningCoreOwnerEventPublisher(
				OwnerEventProperties properties,
				OwnerEventCoreRepository coreRepository,
				OwnerEventDeliveryRepository deliveryRepository,
				OwnerEventConsumerStateRepository stateRepository,
				OwnerEventPublishTransactionService transactionService,
				@Qualifier("learningCoreOwnerEventDeliveryPort") OwnerEventDeliveryPort deliveryPort,
				ObjectMapper objectMapper, MeterRegistry meterRegistry, Clock clock
		) {
			properties.validateLearningCore();
			return publisher(OwnerEventConsumer.LEARNING_CORE, properties, coreRepository,
					deliveryRepository, stateRepository, transactionService,
					deliveryPort, objectMapper, meterRegistry, clock);
		}

		@Bean("learningCoreOwnerEventPublisherScheduler")
		OwnerEventPublisherScheduler learningCoreOwnerEventPublisherScheduler(
				@Qualifier("learningCoreOwnerEventPublisher") OwnerEventPublisher publisher,
				OwnerEventDeliveryRepository deliveryRepository,
				OwnerEventCoreRepository coreRepository,
				OwnerEventProperties properties, Clock clock
		) {
			return new OwnerEventPublisherScheduler(
					publisher, deliveryRepository, coreRepository,
					properties.getMaxBatchSize(), clock);
		}
	}

	private static OwnerEventPublisher publisher(
			OwnerEventConsumer consumer, OwnerEventProperties properties,
			OwnerEventCoreRepository coreRepository,
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository,
			OwnerEventPublishTransactionService transactionService,
			OwnerEventDeliveryPort deliveryPort, ObjectMapper objectMapper,
			MeterRegistry meterRegistry, Clock clock
	) {
		return new OwnerEventPublisher(consumer, properties, coreRepository,
				deliveryRepository, stateRepository, transactionService,
				new OwnerEventWireMapper(objectMapper), deliveryPort,
				new OwnerEventRetryPolicy(properties.getInitialBackoff(),
						properties.getMaxBackoff(),
						() -> ThreadLocalRandom.current().nextDouble()),
				meterRegistry, clock);
	}
}
