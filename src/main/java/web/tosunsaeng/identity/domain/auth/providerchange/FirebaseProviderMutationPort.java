package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Instant;
import java.util.Map;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort;

public interface FirebaseProviderMutationPort {
	record Snapshot(Instant createdAt, Instant validAfter, boolean disabled, Map<SocialProvider, String> providers) {
		public Snapshot { providers = Map.copyOf(providers); }
		@Override public String toString() { return "ProviderSnapshot[REDACTED]"; }
	}
	Snapshot inspect(FirebaseSessionRevocationPort.Target target);
	void unlink(FirebaseSessionRevocationPort.Target target, SocialProvider provider);
	void revoke(FirebaseSessionRevocationPort.Target target, Instant dispatchAt);
}
