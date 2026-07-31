package web.tosunsaeng.identity.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.Document;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;

class MongoTransactionConfigTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MongoTransactionConfig.class)
			.withBean(MongoDatabaseFactory.class, () -> mock(MongoDatabaseFactory.class));

	@Test
	void registersNamedMongoTransactionManagerOutsideTestProfile() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MongoTransactionManager.class);
			assertThat(context).hasBean("mongoTransactionManager");
		});
	}

	@Test
	void testProfileDoesNotCreateExternalMongoTransactionManager() {
		contextRunner
				.withPropertyValues("spring.profiles.active=test")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MongoTransactionManager.class));
	}

	@Test
	void replicaSetWithSessionsPassesTransactionCapabilityVerification() {
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(
				new Document("setName", "test-replica-set")
						.append("logicalSessionTimeoutMinutes", 30)
		);

		new MongoTransactionCapabilityVerifier(mongoTemplate).run(mock(ApplicationArguments.class));
	}

	@Test
	void transactionCapableRouterWithSessionsPassesVerification() {
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(
				new Document("msg", "isdbgrid")
						.append("logicalSessionTimeoutMinutes", 30)
		);

		new MongoTransactionCapabilityVerifier(mongoTemplate).run(mock(ApplicationArguments.class));
	}

	@Test
	void standaloneMongoFailsFastWithoutLeakingServerDetails() {
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(
				new Document("isWritablePrimary", true)
						.append("logicalSessionTimeoutMinutes", 30)
		);

		assertThatThrownBy(() -> new MongoTransactionCapabilityVerifier(mongoTemplate)
				.run(mock(ApplicationArguments.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Guest authentication requires MongoDB transaction support.")
				.hasMessageNotContaining("isWritablePrimary")
				.hasMessageNotContaining("logicalSessionTimeoutMinutes");
	}

	@Test
	void capabilityCommandFailureUsesSafeFixedMessage() {
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		when(mongoTemplate.executeCommand(any(Document.class))).thenThrow(
				new IllegalStateException("test-only internal connection detail")
		);

		assertThatThrownBy(() -> new MongoTransactionCapabilityVerifier(mongoTemplate)
				.run(mock(ApplicationArguments.class)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("MongoDB transaction capability could not be verified.")
				.hasMessageNotContaining("test-only internal connection detail");
	}
}
