package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("app.reissue-recovery")
public class ReissueRecoveryProperties {
	private boolean enabled;
	private boolean maintenance;
	private Duration window = Duration.ofMinutes(2);
	private int responseMaxBytes = 16384;
	private int concurrencyAttempts = 3;
	private String encryptionActiveKeyId;
	private String encryptionKeyringLocation;
	private String environment;
	public void validate() {
		if (!enabled) return;
		if (window == null || window.isZero() || window.isNegative() || window.compareTo(Duration.ofMinutes(2)) > 0
				|| !window.equals(Duration.ofMillis(window.toMillis()))
				|| responseMaxBytes < 1024 || responseMaxBytes > 16384 || concurrencyAttempts < 1 || concurrencyAttempts > 3
				|| environment == null || !environment.matches("[a-zA-Z0-9_-]{1,64}")) {
			throw new IllegalArgumentException("Invalid reissue recovery configuration.");
		}
	}
}
