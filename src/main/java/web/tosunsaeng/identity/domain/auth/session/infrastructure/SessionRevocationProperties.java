package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("app.session-revocation")
public class SessionRevocationProperties {
	private boolean fenceEnabled;
	private boolean captureEnabled;
	private boolean workerEnabled;
	private Duration retention = Duration.ofDays(7);
	private Duration verifierSkew = Duration.ofMinutes(1);
	private Duration lease = Duration.ofSeconds(60);
	private int batchSize = 20;
	private int maxAttempts = 8;
	public void validate() {
		if ((captureEnabled || workerEnabled) && !fenceEnabled) throw new IllegalArgumentException("Session fence is required.");
		if (workerEnabled && !captureEnabled) throw new IllegalArgumentException("Logout capture is required.");
		if (retention == null || retention.isNegative() || retention.isZero()
				|| verifierSkew == null || verifierSkew.isNegative()
				|| lease == null || lease.isNegative() || lease.isZero()
				|| batchSize < 1 || batchSize > 100 || maxAttempts < 1 || maxAttempts > 100) {
			throw new IllegalArgumentException("Invalid session revocation limits.");
		}
	}
}
