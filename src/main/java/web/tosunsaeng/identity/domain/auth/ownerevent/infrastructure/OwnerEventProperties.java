package web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.owner-event")
public class OwnerEventProperties {
	private boolean userMergedCaptureEnabled;
	private boolean trialRebindCaptureEnabled;
	private boolean billingUserMergedPublisherEnabled;
	private boolean learningCoreUserMergedPublisherEnabled;
	private boolean billingTrialRebindPublisherEnabled;
	private URI billingBaseUrl;
	private String billingRegion = "ap-northeast-2";
	private URI learningCoreEndpoint;
	private Duration leaseDuration = Duration.ofSeconds(60);
	private Duration fixedDelay = Duration.ofSeconds(5);
	private Duration initialBackoff = Duration.ofSeconds(5);
	private Duration maxBackoff = Duration.ofMinutes(15);
	private int maxAttempts = 12;
	private Duration publishedRetention = Duration.ofDays(30);
	private Duration deadLetterReview = Duration.ofDays(90);
	private Duration connectTimeout = Duration.ofSeconds(1);
	private Duration readTimeout = Duration.ofSeconds(3);
	private int maxBatchSize = 20;

	public void validateCapture() {
		if (userMergedCaptureEnabled && trialRebindCaptureEnabled) return;
		// An individual capture flag is valid; this method intentionally has no shared dependency.
	}

	public void validateBilling() {
		if (!billingUserMergedPublisherEnabled && !billingTrialRebindPublisherEnabled) return;
		if (!validHttpsOrigin(billingBaseUrl) || billingRegion == null || billingRegion.isBlank()) invalid();
		validateCommon();
	}

	public void validateLearningCore() {
		if (!learningCoreUserMergedPublisherEnabled) return;
		if (learningCoreEndpoint == null || !"https".equalsIgnoreCase(learningCoreEndpoint.getScheme())
				|| learningCoreEndpoint.getHost() == null || learningCoreEndpoint.getUserInfo() != null
				|| learningCoreEndpoint.getQuery() != null || learningCoreEndpoint.getFragment() != null
				|| !LearningCoreOwnerEventDeliveryAdapter.REQUIRED_PATH.equals(
						learningCoreEndpoint.getPath())) invalid();
		validateCommon();
	}

	private void validateCommon() {
		if (!positive(leaseDuration) || !positive(fixedDelay) || !positive(initialBackoff)
				|| !positive(maxBackoff) || initialBackoff.compareTo(maxBackoff) > 0
				|| !positive(publishedRetention) || !positive(deadLetterReview)
				|| !positive(connectTimeout) || !positive(readTimeout)
				|| maxAttempts < 1 || maxBatchSize < 1 || maxBatchSize > 100) invalid();
	}

	private static boolean validHttpsOrigin(URI uri) {
		return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
				&& uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
				&& (uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath()));
	}

	private static boolean positive(Duration duration) {
		return duration != null && !duration.isZero() && !duration.isNegative();
	}

	private static void invalid() {
		throw new IllegalArgumentException("Owner event configuration is invalid.");
	}

	public boolean isUserMergedCaptureEnabled() { return userMergedCaptureEnabled; }
	public void setUserMergedCaptureEnabled(boolean value) { this.userMergedCaptureEnabled = value; }
	public boolean isTrialRebindCaptureEnabled() { return trialRebindCaptureEnabled; }
	public void setTrialRebindCaptureEnabled(boolean value) { this.trialRebindCaptureEnabled = value; }
	public boolean isBillingUserMergedPublisherEnabled() { return billingUserMergedPublisherEnabled; }
	public void setBillingUserMergedPublisherEnabled(boolean value) { this.billingUserMergedPublisherEnabled = value; }
	public boolean isLearningCoreUserMergedPublisherEnabled() { return learningCoreUserMergedPublisherEnabled; }
	public void setLearningCoreUserMergedPublisherEnabled(boolean value) { this.learningCoreUserMergedPublisherEnabled = value; }
	public boolean isBillingTrialRebindPublisherEnabled() { return billingTrialRebindPublisherEnabled; }
	public void setBillingTrialRebindPublisherEnabled(boolean value) { this.billingTrialRebindPublisherEnabled = value; }
	public URI getBillingBaseUrl() { return billingBaseUrl; }
	public void setBillingBaseUrl(URI value) { this.billingBaseUrl = value; }
	public String getBillingRegion() { return billingRegion; }
	public void setBillingRegion(String value) { this.billingRegion = value == null ? "" : value.trim(); }
	public URI getLearningCoreEndpoint() { return learningCoreEndpoint; }
	public void setLearningCoreEndpoint(URI value) { this.learningCoreEndpoint = value; }
	public Duration getLeaseDuration() { return leaseDuration; }
	public void setLeaseDuration(Duration value) { this.leaseDuration = value; }
	public Duration getFixedDelay() { return fixedDelay; }
	public void setFixedDelay(Duration value) { this.fixedDelay = value; }
	public Duration getInitialBackoff() { return initialBackoff; }
	public void setInitialBackoff(Duration value) { this.initialBackoff = value; }
	public Duration getMaxBackoff() { return maxBackoff; }
	public void setMaxBackoff(Duration value) { this.maxBackoff = value; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int value) { this.maxAttempts = value; }
	public Duration getPublishedRetention() { return publishedRetention; }
	public void setPublishedRetention(Duration value) { this.publishedRetention = value; }
	public Duration getDeadLetterReview() { return deadLetterReview; }
	public void setDeadLetterReview(Duration value) { this.deadLetterReview = value; }
	public Duration getConnectTimeout() { return connectTimeout; }
	public void setConnectTimeout(Duration value) { this.connectTimeout = value; }
	public Duration getReadTimeout() { return readTimeout; }
	public void setReadTimeout(Duration value) { this.readTimeout = value; }
	public int getMaxBatchSize() { return maxBatchSize; }
	public void setMaxBatchSize(int value) { this.maxBatchSize = value; }

	@Override public String toString() {
		return "OwnerEventProperties[billingBaseUrl=[REDACTED], learningCoreEndpoint=[REDACTED]]";
	}
}
