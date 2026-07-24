package web.tosunsaeng.identity.user.domain;

import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

	private final EmailNormalizer emailNormalizer;
	private final PasswordEncoder passwordEncoder;
	private final String audioPolicyVersion;

	public UserFactory(
			EmailNormalizer emailNormalizer,
			PasswordEncoder passwordEncoder,
			@Value("${app.consent.audio-policy-version}") String audioPolicyVersion
	) {
		this.emailNormalizer = emailNormalizer;
		this.passwordEncoder = passwordEncoder;
		this.audioPolicyVersion = requireAudioPolicyVersion(audioPolicyVersion);
	}

	public User create(String email, String rawPassword, String nickname) {
		String normalizedEmail = emailNormalizer.normalize(email);
		String passwordHash = passwordEncoder.encode(
				Objects.requireNonNull(rawPassword, "rawPassword must not be null")
		);
		Instant createdAt = Instant.now();
		AudioConsent audioConsent = AudioConsent.agreed(audioPolicyVersion, createdAt);

		return User.create(
				email.trim(),
				normalizedEmail,
				passwordHash,
				nickname,
				audioConsent,
				createdAt
		);
	}

	private String requireAudioPolicyVersion(String policyVersion) {
		String requiredPolicyVersion = Objects.requireNonNull(
				policyVersion,
				"audioPolicyVersion must not be null"
		).trim();
		if (requiredPolicyVersion.isEmpty()) {
			throw new IllegalArgumentException("audioPolicyVersion must not be blank");
		}
		return requiredPolicyVersion;
	}
}
