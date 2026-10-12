package web.tosunsaeng.identity.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class MongoTransactionConfig {

	@Bean(name = "mongoTransactionManager")
	@Profile("!test")
	public MongoTransactionManager mongoTransactionManager(
			MongoDatabaseFactory mongoDatabaseFactory,
			@org.springframework.beans.factory.annotation.Value("${app.purchase.control-writes-enabled:false}") boolean purchaseFence
	) {
		if (!purchaseFence) return new MongoTransactionManager(mongoDatabaseFactory);
		return new MongoTransactionManager(mongoDatabaseFactory, com.mongodb.TransactionOptions.builder()
				.readConcern(com.mongodb.ReadConcern.SNAPSHOT)
				.writeConcern(com.mongodb.WriteConcern.MAJORITY.withJournal(true)).build());
	}
}
