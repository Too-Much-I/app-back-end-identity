package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("app.provider-change")
public class ProviderChangeProperties {

	private boolean fenceEnabled;
	private boolean enabled;
	private boolean workerEnabled;
	private boolean relinkEnabled;
	private boolean linkEnabled;
	private Duration recentAuth = Duration.ofMinutes(5);
	private Duration permitTtl = Duration.ofMinutes(5);
	private Duration retention = Duration.ofDays(7);
	private Duration permitRetention = Duration.ofDays(1);
	private Duration lease = Duration.ofSeconds(60);
	private Duration initialBackoff = Duration.ofSeconds(5);
	private Duration maxBackoff = Duration.ofMinutes(5);
	private int maxAttempts = 12;
	private int batchSize = 20;

	public void validate() {
		if (relinkEnabled) throw new IllegalArgumentException("Legacy relink is retired; migrate the client to common link before enabling link-enabled.");
		if ((enabled || workerEnabled || linkEnabled) && !fenceEnabled) {
			throw new IllegalArgumentException("Provider change requires session fencing.");
		}
		for (Duration duration : new Duration[]{recentAuth, permitTtl, retention, permitRetention,
				lease, initialBackoff, maxBackoff}) {
			if (duration == null || duration.isNegative() || duration.isZero()) {
				throw new IllegalArgumentException("Provider change durations must be positive.");
			}
		}
		if (recentAuth.compareTo(Duration.ofMinutes(5)) > 0 || permitTtl.compareTo(Duration.ofMinutes(5)) > 0
				|| maxAttempts < 1 || maxAttempts > 100 || batchSize < 1 || batchSize > 100
				|| maxBackoff.compareTo(initialBackoff) < 0 || retention.compareTo(Duration.ofDays(7)) < 0) {
			throw new IllegalArgumentException("Invalid provider change limits.");
		}
	}
}
