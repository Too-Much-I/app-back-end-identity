package web.tosunsaeng.identity.domain.auth.providerchange;

import java.io.IOException;
import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseCredentialsProvider;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseProviderMutationHttpAdapter;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProviderChangeProperties.class)
public class ProviderChangeConfiguration {
	@Bean InitializingBean validateProviderChange(ProviderChangeProperties properties) { return properties::validate; }
	@Bean ProviderChangeGuard providerChangeGuard(MongoTemplate mongo) { return new ProviderChangeGuard(mongo); }
	@Bean
	@ConditionalOnProperty(prefix = "app.provider-change", name = "fence-enabled", havingValue = "true")
	ProviderLinkService providerLinkService(ProviderChangeService changes, MongoTemplate mongo, SessionSecurityService security,
			ProviderChangeGuard guard, SocialIdentityRepository socials, ProviderChangeProperties properties, Clock clock) {
		return new ProviderLinkService(changes, mongo, security, guard, socials, properties, clock);
	}
	@Bean
	@ConditionalOnProperty(prefix = "app.provider-change", name = "fence-enabled", havingValue = "true")
	ProviderChangeService providerChangeService(MongoTemplate mongo, MongoTransactionManager manager, SessionSecurityService security,
			ProviderChangeGuard guard, FirebaseAuthenticationVerifier verifier, FirebaseIdentityRepository identities,
			SocialIdentityRepository socials, UserRepository users, ProviderChangeProperties properties, FirebaseAuthProperties firebase, Clock clock) {
		if (!firebase.enabled()) throw new IllegalArgumentException("Provider change requires Firebase authentication.");
		var tx = new TransactionTemplate(manager); tx.setTimeout(10);
		return new ProviderChangeService(mongo, tx, security, guard, verifier, identities, socials, users, properties, clock, firebase.tenantId());
	}
	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(prefix = "app.provider-change", name = "worker-enabled", havingValue = "true")
	static class WorkerConfiguration {
		@Bean FirebaseProviderMutationPort firebaseProviderMutationPort(FirebaseCredentialsProvider credentials,
				FirebaseAuthProperties firebase, ObjectMapper json) throws IOException {
			return new FirebaseProviderMutationHttpAdapter(new NetHttpTransport(), credentials.load(), firebase, json);
		}
		@Bean ProviderUnlinkWorker providerUnlinkWorker(MongoTemplate mongo, SessionSecurityService security, FirebaseIdentityRepository identities,
				SocialIdentityRepository socials, FirebaseProviderMutationPort port, ProviderChangeProperties properties, FirebaseAuthProperties firebase, Clock clock) {
			return new ProviderUnlinkWorker(mongo, security, identities, socials, port, properties, firebase.tenantId(), clock);
		}
		@Bean Scheduler providerUnlinkScheduler(ProviderUnlinkWorker worker) { return new Scheduler(worker); }
	}
	static class Scheduler {
		private final ProviderUnlinkWorker worker;
		Scheduler(ProviderUnlinkWorker worker) { this.worker = worker; }
		@Scheduled(fixedDelayString = "${app.provider-change.poll-delay:PT5S}")
		public void run() {
			try { worker.runBatch(); }
			catch (RuntimeException exception) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Provider change scheduler unavailable."); }
		}
	}
}
