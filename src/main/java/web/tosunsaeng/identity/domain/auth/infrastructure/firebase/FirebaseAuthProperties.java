package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.firebase-auth")
public record FirebaseAuthProperties(
		boolean enabled,
		String projectId,
		String tenantId,
		boolean googleEnabled,
		boolean appleEnabled,
		boolean kakaoEnabled,
		boolean phoneEnabled,
		String kakaoProviderId,
		Duration loginMaxAuthenticationAge,
		Duration highRiskMaxAuthenticationAge,
		Duration clockSkew,
		Duration connectTimeout,
		Duration readTimeout,
		Duration enrollmentTtl,
		Duration enrollmentCleanupRetention
) {

	private static final String DEFAULT_KAKAO_PROVIDER_ID = "oidc.kakao";
	private static final Duration DEFAULT_LOGIN_MAX_AGE = Duration.ofMinutes(15);
	private static final Duration DEFAULT_HIGH_RISK_MAX_AGE = Duration.ofMinutes(5);
	private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(30);
	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration DEFAULT_ENROLLMENT_TTL = Duration.ofMinutes(10);
	private static final Duration DEFAULT_ENROLLMENT_CLEANUP_RETENTION = Duration.ofHours(24);

	public FirebaseAuthProperties {
		projectId = normalizeOptional(projectId);
		tenantId = normalizeOptional(tenantId);
		kakaoProviderId = normalizeOptional(kakaoProviderId);
		if (kakaoProviderId == null) {
			kakaoProviderId = DEFAULT_KAKAO_PROVIDER_ID;
		}
		loginMaxAuthenticationAge = defaultIfNull(
				loginMaxAuthenticationAge,
				DEFAULT_LOGIN_MAX_AGE
		);
		highRiskMaxAuthenticationAge = defaultIfNull(
				highRiskMaxAuthenticationAge,
				DEFAULT_HIGH_RISK_MAX_AGE
		);
		clockSkew = defaultIfNull(clockSkew, DEFAULT_CLOCK_SKEW);
		connectTimeout = defaultIfNull(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
		readTimeout = defaultIfNull(readTimeout, DEFAULT_READ_TIMEOUT);
		enrollmentTtl = defaultIfNull(enrollmentTtl, DEFAULT_ENROLLMENT_TTL);
		enrollmentCleanupRetention = defaultIfNull(
				enrollmentCleanupRetention,
				DEFAULT_ENROLLMENT_CLEANUP_RETENTION
		);

		if (enabled && projectId == null) {
			throw new IllegalArgumentException(
					"app.firebase-auth.project-id is required when Firebase authentication is enabled"
			);
		}
		if (kakaoEnabled && !kakaoProviderId.startsWith("oidc.")) {
			throw new IllegalArgumentException("Kakao Firebase provider ID must start with oidc.");
		}
		requirePositive(loginMaxAuthenticationAge, "loginMaxAuthenticationAge");
		requirePositive(highRiskMaxAuthenticationAge, "highRiskMaxAuthenticationAge");
		requireNonNegative(clockSkew, "clockSkew");
		requirePositiveMillis(connectTimeout, "connectTimeout");
		requirePositiveMillis(readTimeout, "readTimeout");
		requirePositive(enrollmentTtl, "enrollmentTtl");
		requirePositive(enrollmentCleanupRetention, "enrollmentCleanupRetention");
	}

	public String expectedIssuer() {
		if (projectId == null) {
			throw new IllegalStateException("Firebase project is not configured.");
		}
		return "https://securetoken.google.com/" + projectId;
	}

	public int connectTimeoutMillis() {
		return Math.toIntExact(connectTimeout.toMillis());
	}

	public int readTimeoutMillis() {
		return Math.toIntExact(readTimeout.toMillis());
	}

	private static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static Duration defaultIfNull(Duration value, Duration defaultValue) {
		return value == null ? defaultValue : value;
	}

	private static void requirePositive(Duration value, String fieldName) {
		if (value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
	}

	private static void requireNonNegative(Duration value, String fieldName) {
		if (value.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
	}

	private static void requirePositiveMillis(Duration value, String fieldName) {
		requirePositive(value, fieldName);
		long milliseconds = value.toMillis();
		if (milliseconds <= 0 || milliseconds > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(fieldName + " must fit in positive milliseconds");
		}
	}

	@Override
	public String toString() {
		return "FirebaseAuthProperties[enabled=" + enabled
				+ ", projectId=[REDACTED], tenantId=[REDACTED]"
				+ ", googleEnabled=" + googleEnabled
				+ ", appleEnabled=" + appleEnabled
				+ ", kakaoEnabled=" + kakaoEnabled
				+ ", phoneEnabled=" + phoneEnabled + "]";
	}
}
