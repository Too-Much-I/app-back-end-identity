package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import java.io.IOException;
import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseCredentialsProvider;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseSessionRevocationHttpAdapter;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.auth.session.application.LogoutAllCoordinator;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationWorker;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SessionRevocationProperties.class)
public class SessionRevocationConfiguration {
	@Bean
	org.springframework.beans.factory.InitializingBean validateSessionRevocationProperties(SessionRevocationProperties properties) {
		return properties::validate;
	}
	@Bean
	@ConditionalOnProperty(prefix = "app.session-revocation", name = "fence-enabled", havingValue = "true")
	SessionSecurityService sessionSecurityService(MongoTemplate mongo, MongoTransactionManager manager,
			UserRepository users, FirebaseIdentityRepository identities, SessionRevocationProperties properties) {
		properties.validate();
		var service = new SessionSecurityService(mongo, new TransactionTemplate(manager), users, identities);
		service.configureRetention(properties.getRetention(), properties.getVerifierSkew());
		return service;
	}
	@Bean
	@ConditionalOnProperty(prefix = "app.session-revocation", name = "capture-enabled", havingValue = "true")
	LogoutAllCoordinator logoutAllCoordinator(SessionSecurityService security, MongoTemplate mongo,
			FirebaseIdentityRepository identities, SessionRevocationProperties properties, FirebaseAuthProperties firebase, Clock clock) {
		properties.validate();
		if (!firebase.enabled()) throw new IllegalArgumentException("Firebase must be configured for logout capture.");
		return new LogoutAllCoordinator(security, mongo, identities, properties, firebase.tenantId(), clock);
	}
	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(prefix = "app.session-revocation", name = "worker-enabled", havingValue = "true")
	static class WorkerConfiguration {
		@Bean
		FirebaseSessionRevocationPort firebaseSessionRevocationPort(FirebaseCredentialsProvider credentials,
				FirebaseAuthProperties firebase, ObjectMapper json) throws IOException {
			return new FirebaseSessionRevocationHttpAdapter(new NetHttpTransport(), credentials.load(), firebase, json);
		}
		@Bean
		FirebaseSessionRevocationWorker firebaseSessionRevocationWorker(SessionSecurityService security, MongoTemplate mongo,
				FirebaseIdentityRepository identities, FirebaseSessionRevocationPort port, SessionRevocationProperties properties,
				LogoutAllCoordinator coordinator, Clock clock) {
			return new FirebaseSessionRevocationWorker(security, mongo, identities, port, properties, coordinator, clock);
		}
		@Bean Scheduler sessionRevocationScheduler(FirebaseSessionRevocationWorker worker) { return new Scheduler(worker); }
	}
	static class Scheduler {
		private final FirebaseSessionRevocationWorker worker;
		Scheduler(FirebaseSessionRevocationWorker worker) { this.worker = worker; }
		@Scheduled(fixedDelayString = "${app.session-revocation.poll-delay:PT5S}")
		public void run() {
			try { worker.runBatch(); }
			catch (RuntimeException exception) {
				org.slf4j.LoggerFactory.getLogger(Scheduler.class).warn("Session revocation batch could not be completed.");
			}
		}
	}
}
