package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.account-recovery")
public record RecoveryProperties(boolean enabled, String keyRing, Duration challengeTtl,
		Duration recentAuth, Duration retryTtl, int preparePerMinute, int lookupPerMinute, int proofPerQuarterHour) {
	public RecoveryProperties {
		challengeTtl = challengeTtl == null ? Duration.ofMinutes(5) : challengeTtl;
		recentAuth = recentAuth == null ? Duration.ofMinutes(5) : recentAuth;
		retryTtl = retryTtl == null ? Duration.ofMinutes(5) : retryTtl;
		preparePerMinute = preparePerMinute == 0 ? 10 : preparePerMinute;
		lookupPerMinute = lookupPerMinute == 0 ? 20 : lookupPerMinute;
		proofPerQuarterHour = proofPerQuarterHour == 0 ? 5 : proofPerQuarterHour;
		for (Duration d : new Duration[]{challengeTtl, recentAuth, retryTtl}) {
			if (d.isNegative() || d.isZero() || d.compareTo(Duration.ofMinutes(15)) > 0) throw new IllegalArgumentException("Recovery durations must be within 15 minutes");
		}
		if (preparePerMinute < 1 || lookupPerMinute < 1 || proofPerQuarterHour < 1) throw new IllegalArgumentException("Recovery limits must be positive");
	}
	@Override public String toString() { return "RecoveryProperties[enabled=" + enabled + ", keys=REDACTED]"; }
}
