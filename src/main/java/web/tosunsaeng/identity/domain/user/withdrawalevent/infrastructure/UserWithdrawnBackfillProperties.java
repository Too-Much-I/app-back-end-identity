package web.tosunsaeng.identity.domain.user.withdrawalevent.infrastructure;

import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.user-withdrawn-backfill")
public class UserWithdrawnBackfillProperties {

	private boolean enabled;
	private boolean dryRun = true;
	private Instant lowerBound;
	private Instant upperBound;
	private int batchLimit = 20;

	public void validate() {
		if (!enabled) {
			return;
		}
		if (lowerBound == null
				|| upperBound == null
				|| !upperBound.isAfter(lowerBound)
				|| batchLimit < 1
				|| batchLimit > 100) {
			throw new IllegalArgumentException("UserWithdrawn backfill configuration is invalid.");
		}
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public boolean isDryRun() { return dryRun; }
	public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
	public Instant getLowerBound() { return lowerBound; }
	public void setLowerBound(Instant lowerBound) { this.lowerBound = lowerBound; }
	public Instant getUpperBound() { return upperBound; }
	public void setUpperBound(Instant upperBound) { this.upperBound = upperBound; }
	public int getBatchLimit() { return batchLimit; }
	public void setBatchLimit(int batchLimit) { this.batchLimit = batchLimit; }

	@Override
	public String toString() {
		return "UserWithdrawnBackfillProperties[enabled=" + enabled
				+ ", dryRun=" + dryRun + ", bounds=[REDACTED], batchLimit="
				+ batchLimit + "]";
	}
}
