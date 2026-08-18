package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.phone-eligibility-publisher")
public class PhoneEligibilityPublisherProperties {

	private boolean enabled;
	private URI endpoint;
	private String audience = "";
	private Duration leaseDuration = Duration.ofSeconds(60);
	private Duration fixedDelay = Duration.ofSeconds(5);
	private int maxAttempts = 12;
	private Duration publishedRetention = Duration.ofDays(30);
	private Duration deadLetterReview = Duration.ofDays(90);
	private Duration connectTimeout = Duration.ofSeconds(3);
	private Duration readTimeout = Duration.ofSeconds(5);
	private int maxBatchSize = 20;

	public void validate(PhoneEligibilityBindingProperties bindingProperties) {
		if (!enabled) return;
		if (!bindingProperties.enabled()) throw invalid();
		if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())
				|| endpoint.getHost() == null || endpoint.getUserInfo() != null
				|| endpoint.getFragment() != null || audience == null || audience.isBlank()
				|| !positive(leaseDuration) || !positive(fixedDelay)
				|| !positive(publishedRetention) || !positive(deadLetterReview)
				|| !positive(connectTimeout) || !positive(readTimeout)
				|| maxAttempts < 1 || maxBatchSize < 1 || maxBatchSize > 100) {
			throw invalid();
		}
	}

	private static boolean positive(Duration value) {
		return value != null && !value.isZero() && !value.isNegative();
	}

	private static IllegalArgumentException invalid() {
		return new IllegalArgumentException("Phone eligibility publisher configuration is invalid.");
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public URI getEndpoint() { return endpoint; }
	public void setEndpoint(URI endpoint) { this.endpoint = endpoint; }
	public String getAudience() { return audience; }
	public void setAudience(String audience) { this.audience = audience == null ? "" : audience.trim(); }
	public Duration getLeaseDuration() { return leaseDuration; }
	public void setLeaseDuration(Duration leaseDuration) { this.leaseDuration = leaseDuration; }
	public Duration getFixedDelay() { return fixedDelay; }
	public void setFixedDelay(Duration fixedDelay) { this.fixedDelay = fixedDelay; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
	public Duration getPublishedRetention() { return publishedRetention; }
	public void setPublishedRetention(Duration publishedRetention) { this.publishedRetention = publishedRetention; }
	public Duration getDeadLetterReview() { return deadLetterReview; }
	public void setDeadLetterReview(Duration deadLetterReview) { this.deadLetterReview = deadLetterReview; }
	public Duration getConnectTimeout() { return connectTimeout; }
	public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
	public Duration getReadTimeout() { return readTimeout; }
	public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
	public int getMaxBatchSize() { return maxBatchSize; }
	public void setMaxBatchSize(int maxBatchSize) { this.maxBatchSize = maxBatchSize; }

	@Override
	public String toString() {
		return "PhoneEligibilityPublisherProperties[enabled=" + enabled
				+ ", endpoint=[REDACTED], audience=[REDACTED]]";
	}
}
