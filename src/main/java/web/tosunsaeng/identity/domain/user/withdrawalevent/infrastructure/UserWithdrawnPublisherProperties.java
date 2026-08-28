package web.tosunsaeng.identity.domain.user.withdrawalevent.infrastructure;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import web.tosunsaeng.identity.global.security.jwt.WorkloadJwtProperties;

@ConfigurationProperties(prefix = "app.user-withdrawn-publisher")
public class UserWithdrawnPublisherProperties {

	private static final String REQUIRED_PATH = "/internal/v1/events/withdrawn";

	private boolean enabled;
	private URI endpoint;
	private String audience = WorkloadJwtProperties.REQUIRED_AUDIENCE;
	private Duration leaseDuration = Duration.ofSeconds(60);
	private Duration fixedDelay = Duration.ofSeconds(5);
	private int maxAttempts = 12;
	private Duration publishedRetention = Duration.ofDays(30);
	private Duration deadLetterReview = Duration.ofDays(90);
	private Duration connectTimeout = Duration.ofSeconds(3);
	private Duration readTimeout = Duration.ofSeconds(5);
	private int maxBatchSize = 20;

	public void validate() {
		if (!enabled) {
			return;
		}
		if (endpoint == null
				|| !"https".equalsIgnoreCase(endpoint.getScheme())
				|| endpoint.getHost() == null
				|| endpoint.getUserInfo() != null
				|| endpoint.getFragment() != null
				|| endpoint.getQuery() != null
				|| !REQUIRED_PATH.equals(endpoint.getPath())
				|| !WorkloadJwtProperties.REQUIRED_AUDIENCE.equals(audience)
				|| !positive(leaseDuration)
				|| !positive(fixedDelay)
				|| !positive(publishedRetention)
				|| !positive(deadLetterReview)
				|| !positive(connectTimeout)
				|| !positive(readTimeout)
				|| maxAttempts < 1
				|| maxBatchSize < 1
				|| maxBatchSize > 100) {
			throw new IllegalArgumentException("UserWithdrawn publisher configuration is invalid.");
		}
	}

	private static boolean positive(Duration value) {
		return value != null && !value.isZero() && !value.isNegative();
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
	public void setPublishedRetention(Duration value) { this.publishedRetention = value; }
	public Duration getDeadLetterReview() { return deadLetterReview; }
	public void setDeadLetterReview(Duration value) { this.deadLetterReview = value; }
	public Duration getConnectTimeout() { return connectTimeout; }
	public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
	public Duration getReadTimeout() { return readTimeout; }
	public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
	public int getMaxBatchSize() { return maxBatchSize; }
	public void setMaxBatchSize(int maxBatchSize) { this.maxBatchSize = maxBatchSize; }

	@Override
	public String toString() {
		return "UserWithdrawnPublisherProperties[enabled=" + enabled
				+ ", endpoint=[REDACTED], audience=[REDACTED]]";
	}
}
