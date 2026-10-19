package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.repository.*;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReissueRecoveryProperties.class)
public class ReissueRecoveryConfiguration {
	@Bean
	@ConditionalOnProperty(prefix = "app.reissue-recovery", name = "enabled", havingValue = "true")
	ReissueEncryptionKeyProvider reissueEncryptionKeyProvider(ReissueRecoveryProperties properties, SessionSecurityService security) {
		properties.validate(); // Security bean is deliberately mandatory: no fence-disabled recovery.
		return new MountedReissueEncryptionKeys(properties.getEncryptionActiveKeyId(), properties.getEncryptionKeyringLocation());
	}
	@Bean
	@ConditionalOnProperty(prefix = "app.reissue-recovery", name = "enabled", havingValue = "true")
	ReissueResponseCipher reissueResponseCipher(ReissueEncryptionKeyProvider keys, ObjectMapper json, ReissueRecoveryProperties properties) {
		return new AesGcmReissueResponseCipher(keys, json, properties.getEnvironment(), properties.getResponseMaxBytes());
	}
	@Bean
	@ConditionalOnProperty(prefix = "app.reissue-recovery", name = "enabled", havingValue = "true")
	ReissueRecoveryService reissueRecoveryService(RefreshSessionRepository sessions, RefreshReissueResponseRepository responses,
			UserRepository users, RefreshTokenHasher hasher, RefreshSessionIssuer refreshIssuer, AccessTokenIssuer accessIssuer,
			AuthResponseConverter converter, SessionSecurityService security, MongoTransactionManager manager,
			ReissueResponseCipher cipher, ReissueRecoveryProperties properties, Clock clock, MeterRegistry metrics) {
		// Use the same database resource as the common fence, with explicit durable commit semantics.
		var recoveryManager = new MongoTransactionManager(manager.getDatabaseFactory(), com.mongodb.TransactionOptions.builder()
				.readConcern(com.mongodb.ReadConcern.SNAPSHOT).writeConcern(com.mongodb.WriteConcern.MAJORITY)
				.readPreference(com.mongodb.ReadPreference.primary()).maxCommitTime(5L, java.util.concurrent.TimeUnit.SECONDS).build());
		var tx = new TransactionTemplate(recoveryManager);
		// Recovery owns the commit boundary and must not return inside an outer transaction.
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tx.setTimeout(10);
		return new ReissueRecoveryService(sessions, responses, users, hasher, refreshIssuer, accessIssuer,
				converter, security, tx, cipher, properties, clock, metrics);
	}
}
