package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.federation.repository.*;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.*;
import web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard;
import web.tosunsaeng.identity.domain.user.domain.repository.*;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(RecoveryProperties.class)
@ConditionalOnProperty(prefix="app.account-recovery", name="enabled", havingValue="true")
public class AccountRecoveryConfiguration {
	@Bean AccountRecoveryService accountRecoveryService(MongoTemplate mongo, MongoTransactionManager manager,
			RecoveryProperties properties, FirebaseAuthProperties firebase, FirebaseAuthenticationVerifier verifier,
			PhoneFingerprintHasher phones, PhoneFingerprintAliasRepository aliases, PhoneIdentityRepository phoneIdentities,
			UserRepository users, SocialIdentityRepository socials, FirebaseIdentityRepository bindings,
			UserWithdrawalLifecycleRepository withdrawals, ProviderChangeGuard guard, Clock clock) {
		if (!firebase.enabled() || !firebase.phoneEnabled()) throw new IllegalArgumentException("Account recovery requires Firebase SMS authentication");
		var tx = new TransactionTemplate(manager); tx.setTimeout(10);
		return new AccountRecoveryService(new MongoRecoveryStore(mongo, tx), new RecoveryHasher(properties.keyRing()), verifier,
				new RecoveryAccountResolver(phones, aliases, phoneIdentities, users, socials, bindings, withdrawals, guard), properties, clock, firebase.tenantId());
	}
}
