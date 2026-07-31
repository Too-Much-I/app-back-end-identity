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
			MongoDatabaseFactory mongoDatabaseFactory
	) {
		return new MongoTransactionManager(mongoDatabaseFactory);
	}
}
