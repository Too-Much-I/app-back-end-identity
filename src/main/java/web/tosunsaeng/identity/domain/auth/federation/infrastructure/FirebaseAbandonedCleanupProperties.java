package web.tosunsaeng.identity.domain.auth.federation.infrastructure;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.firebase-abandoned-cleanup")
public class FirebaseAbandonedCleanupProperties {

	private boolean captureEnabled;
	private boolean workerEnabled;
	private boolean captureDryRun = true;
	private Instant captureLowerBound;
	private Instant captureUpperBound;
	private Duration grace = Duration.ofHours(24);
	private Duration terminalLifecycleRetention = Duration.ofDays(7);
	private Duration terminalEnrollmentRetention = Duration.ofHours(24);
	private Duration fixedDelay = Duration.ofSeconds(5);
	private Duration leaseDuration = Duration.ofMinutes(1);
	private int maxAttempts = 12;
	private Duration initialBackoff = Duration.ofSeconds(5);
	private Duration maxBackoff = Duration.ofHours(1);
	private int maxBatchSize = 20;
	private int captureMaxBatchSize = 100;

	public void validate() {
		if (!positive(grace)
				|| !positive(terminalLifecycleRetention)
				|| !positive(terminalEnrollmentRetention)
				|| terminalLifecycleRetention.compareTo(terminalEnrollmentRetention) <= 0
				|| !positive(fixedDelay)
				|| !positive(leaseDuration)
				|| !positive(initialBackoff)
				|| !positive(maxBackoff)
				|| initialBackoff.compareTo(maxBackoff) > 0
				|| maxAttempts < 1
				|| maxBatchSize < 1
				|| maxBatchSize > 100
				|| captureMaxBatchSize < 1
				|| captureMaxBatchSize > 100
				|| (captureEnabled && (captureLowerBound == null
						|| captureUpperBound == null
						|| !captureUpperBound.isAfter(captureLowerBound)))) {
			throw new IllegalArgumentException("Firebase abandoned cleanup configuration is invalid.");
		}
	}

	private static boolean positive(Duration value) {
		return value != null && !value.isZero() && !value.isNegative();
	}

	public boolean isCaptureEnabled() { return captureEnabled; }
	public void setCaptureEnabled(boolean captureEnabled) { this.captureEnabled = captureEnabled; }
	public boolean isWorkerEnabled() { return workerEnabled; }
	public void setWorkerEnabled(boolean workerEnabled) { this.workerEnabled = workerEnabled; }
	public boolean isCaptureDryRun() { return captureDryRun; }
	public void setCaptureDryRun(boolean captureDryRun) { this.captureDryRun = captureDryRun; }
	public Instant getCaptureLowerBound() { return captureLowerBound; }
	public void setCaptureLowerBound(Instant value) { captureLowerBound = value; }
	public Instant getCaptureUpperBound() { return captureUpperBound; }
	public void setCaptureUpperBound(Instant value) { captureUpperBound = value; }
	public Duration getGrace() { return grace; }
	public void setGrace(Duration grace) { this.grace = grace; }
	public Duration getTerminalLifecycleRetention() { return terminalLifecycleRetention; }
	public void setTerminalLifecycleRetention(Duration value) { terminalLifecycleRetention = value; }
	public Duration getTerminalEnrollmentRetention() { return terminalEnrollmentRetention; }
	public void setTerminalEnrollmentRetention(Duration value) { terminalEnrollmentRetention = value; }
	public Duration getFixedDelay() { return fixedDelay; }
	public void setFixedDelay(Duration fixedDelay) { this.fixedDelay = fixedDelay; }
	public Duration getLeaseDuration() { return leaseDuration; }
	public void setLeaseDuration(Duration leaseDuration) { this.leaseDuration = leaseDuration; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
	public Duration getInitialBackoff() { return initialBackoff; }
	public void setInitialBackoff(Duration initialBackoff) { this.initialBackoff = initialBackoff; }
	public Duration getMaxBackoff() { return maxBackoff; }
	public void setMaxBackoff(Duration maxBackoff) { this.maxBackoff = maxBackoff; }
	public int getMaxBatchSize() { return maxBatchSize; }
	public void setMaxBatchSize(int maxBatchSize) { this.maxBatchSize = maxBatchSize; }
	public int getCaptureMaxBatchSize() { return captureMaxBatchSize; }
	public void setCaptureMaxBatchSize(int value) { captureMaxBatchSize = value; }

	@Override
	public String toString() {
		return "FirebaseAbandonedCleanupProperties[captureEnabled=" + captureEnabled
				+ ", workerEnabled=" + workerEnabled
				+ ", grace=" + grace
				+ ", terminalLifecycleRetention=" + terminalLifecycleRetention
				+ ", terminalEnrollmentRetention=" + terminalEnrollmentRetention
				+ ", fixedDelay=" + fixedDelay
				+ ", leaseDuration=" + leaseDuration
				+ ", maxAttempts=" + maxAttempts
				+ ", maxBatchSize=" + maxBatchSize + "]";
	}
}
