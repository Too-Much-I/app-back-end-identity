package web.tosunsaeng.identity.domain.user.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase-withdrawal-identity-release")
public class UserWithdrawalIdentityReleaseProperties {

	private boolean enabled;
	private Duration fixedDelay = Duration.ofSeconds(5);
	private int maxBatchSize = 20;

	public void validate() {
		if (!enabled) return;
		if (fixedDelay == null
				|| fixedDelay.isZero()
				|| fixedDelay.isNegative()
				|| fixedDelay.toMillis() <= 0
				|| maxBatchSize < 1
				|| maxBatchSize > 100) {
			throw new IllegalArgumentException(
					"Firebase withdrawal identity release configuration is invalid."
			);
		}
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public Duration getFixedDelay() { return fixedDelay; }
	public void setFixedDelay(Duration fixedDelay) { this.fixedDelay = fixedDelay; }
	public int getMaxBatchSize() { return maxBatchSize; }
	public void setMaxBatchSize(int maxBatchSize) { this.maxBatchSize = maxBatchSize; }
}
