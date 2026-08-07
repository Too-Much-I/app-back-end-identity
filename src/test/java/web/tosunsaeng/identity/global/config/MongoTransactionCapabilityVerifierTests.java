package web.tosunsaeng.identity.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.Document;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;

import web.tosunsaeng.identity.support.LogCapture;

class MongoTransactionCapabilityVerifierTests {

	@Test
	void logsOnlySafeTopologyCategoryAfterCapabilityVerification() {
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		when(mongoTemplate.executeCommand(any(Document.class))).thenReturn(
				new Document("setName", "test-replica-set")
						.append("logicalSessionTimeoutMinutes", 30)
		);
		MongoTransactionCapabilityVerifier verifier =
				new MongoTransactionCapabilityVerifier(mongoTemplate);

		try (LogCapture logs = LogCapture.forClass(
				MongoTransactionCapabilityVerifier.class
		)) {
			verifier.run(mock(ApplicationArguments.class));

			assertThat(logs.events("mongodb.transaction_capability.verified"))
					.singleElement()
					.satisfies(event -> {
						assertThat(LogCapture.value(event, "outcome")).isEqualTo("supported");
						assertThat(LogCapture.value(event, "topology")).isEqualTo("replica_set");
						assertThat(LogCapture.value(event, "sessionsSupported")).isEqualTo(true);
						assertThat(LogCapture.rendered(event))
								.doesNotContain("test-replica-set");
					});
		}
	}
}
