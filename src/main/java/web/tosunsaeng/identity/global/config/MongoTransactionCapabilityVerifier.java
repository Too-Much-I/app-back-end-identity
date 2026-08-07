package web.tosunsaeng.identity.global.config;

import org.bson.Document;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class MongoTransactionCapabilityVerifier implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(
			MongoTransactionCapabilityVerifier.class
	);
	private static final String VERIFICATION_FAILED =
			"MongoDB transaction capability could not be verified.";
	private static final String TRANSACTION_REQUIRED =
			"Guest authentication requires MongoDB transaction support.";

	private final MongoTemplate mongoTemplate;

	@Override
	public void run(ApplicationArguments args) {
		Document hello;
		try {
			hello = mongoTemplate.executeCommand(new Document("hello", 1));
			if (hello == null) {
				throw new IllegalStateException(VERIFICATION_FAILED);
			}
		} catch (RuntimeException exception) {
			throw new IllegalStateException(VERIFICATION_FAILED);
		}

		boolean transactionCapableTopology = hello.getString("setName") != null
				|| "isdbgrid".equals(hello.getString("msg"));
		boolean sessionsSupported = hello.get("logicalSessionTimeoutMinutes") instanceof Number;
		if (!transactionCapableTopology || !sessionsSupported) {
			throw new IllegalStateException(TRANSACTION_REQUIRED);
		}

		log.atInfo()
				.addKeyValue("event", "mongodb.transaction_capability.verified")
				.addKeyValue("outcome", "supported")
				.addKeyValue(
						"topology",
						hello.getString("setName") != null ? "replica_set" : "sharded"
				)
				.addKeyValue("sessionsSupported", true)
				.log("MongoDB transaction capability verified");
	}
}
