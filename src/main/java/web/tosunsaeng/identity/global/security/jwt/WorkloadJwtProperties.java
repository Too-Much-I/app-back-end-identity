package web.tosunsaeng.identity.global.security.jwt;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workload-jwt")
public class WorkloadJwtProperties {

	public static final String REQUIRED_AUDIENCE = "learning-core-user-withdrawn";
	public static final String REQUIRED_SUBJECT = "identity-service";
	public static final Duration REQUIRED_TTL = Duration.ofMinutes(2);

	private boolean enabled;
	private String issuer = "";
	private String audience = REQUIRED_AUDIENCE;
	private String subject = REQUIRED_SUBJECT;
	private Duration ttl = REQUIRED_TTL;

	public void validate() {
		if (!enabled) {
			return;
		}
		if (!validHttpsIssuer(issuer)
				|| !REQUIRED_AUDIENCE.equals(audience)
				|| !REQUIRED_SUBJECT.equals(subject)
				|| !REQUIRED_TTL.equals(ttl)) {
			throw new IllegalArgumentException("Workload JWT configuration is invalid.");
		}
	}

	private static boolean validHttpsIssuer(String value) {
		try {
			URI uri = URI.create(value);
			return "https".equalsIgnoreCase(uri.getScheme())
					&& uri.getHost() != null
					&& uri.getUserInfo() == null
					&& uri.getQuery() == null
					&& uri.getFragment() == null;
		} catch (IllegalArgumentException | NullPointerException exception) {
			return false;
		}
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getIssuer() { return issuer; }
	public void setIssuer(String issuer) { this.issuer = issuer == null ? "" : issuer.trim(); }
	public String getAudience() { return audience; }
	public void setAudience(String audience) { this.audience = audience == null ? "" : audience.trim(); }
	public String getSubject() { return subject; }
	public void setSubject(String subject) { this.subject = subject == null ? "" : subject.trim(); }
	public Duration getTtl() { return ttl; }
	public void setTtl(Duration ttl) { this.ttl = ttl; }

	@Override
	public String toString() {
		return "WorkloadJwtProperties[enabled=" + enabled
				+ ", issuer=[REDACTED], audience=[REDACTED], subject=" + subject
				+ ", ttl=" + ttl + "]";
	}
}
