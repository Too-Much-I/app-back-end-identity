package web.tosunsaeng.identity.domain.user.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;

@ConfigurationProperties(prefix = "app.firebase-withdrawal-cleanup")
public class UserWithdrawalExternalCleanupProperties {

	private boolean enabled;
	private Duration fixedDelay = Duration.ofSeconds(5);
	private Duration leaseDuration = Duration.ofMinutes(1);
	private int maxAttempts = 12;
	private Duration initialBackoff = Duration.ofSeconds(5);
	private Duration maxBackoff = Duration.ofHours(1);
	private int maxBatchSize = 20;
	private Duration connectTimeout = Duration.ofSeconds(3);
	private Duration readTimeout = Duration.ofSeconds(5);

	public void validate(FirebaseAuthProperties firebaseAuthProperties) {
		if (!enabled) return;
		if (firebaseAuthProperties == null
				|| !firebaseAuthProperties.enabled()
				|| firebaseAuthProperties.projectId() == null
				|| !positive(fixedDelay)
				|| !positive(leaseDuration)
				|| !positive(initialBackoff)
				|| !positive(maxBackoff)
				|| !positiveMillis(connectTimeout)
				|| !positiveMillis(readTimeout)
				|| initialBackoff.compareTo(maxBackoff) > 0
				|| maxAttempts < 1
				|| maxBatchSize < 1
				|| maxBatchSize > 100) {
			throw invalid();
		}
		Duration oneCallBudget = connectTimeout.plus(readTimeout);
		if (leaseDuration.compareTo(oneCallBudget) <= 0) {
			throw invalid();
		}
	}

	private static boolean positive(Duration value) {
		return value != null && !value.isZero() && !value.isNegative() && value.toMillis() > 0;
	}

	private static boolean positiveMillis(Duration value) {
		return positive(value) && value.toMillis() <= Integer.MAX_VALUE;
	}

	private static IllegalArgumentException invalid() {
		return new IllegalArgumentException(
				"Firebase withdrawal cleanup configuration is invalid."
		);
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
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
	public Duration getConnectTimeout() { return connectTimeout; }
	public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
	public Duration getReadTimeout() { return readTimeout; }
	public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

	@Override
	public String toString() {
		return "UserWithdrawalExternalCleanupProperties[enabled=" + enabled
				+ ", fixedDelay=" + fixedDelay
				+ ", leaseDuration=" + leaseDuration
				+ ", maxAttempts=" + maxAttempts
				+ ", initialBackoff=" + initialBackoff
				+ ", maxBackoff=" + maxBackoff
				+ ", maxBatchSize=" + maxBatchSize
				+ ", connectTimeout=" + connectTimeout
				+ ", readTimeout=" + readTimeout + "]";
	}
}
