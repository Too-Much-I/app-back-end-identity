package web.tosunsaeng.identity.domain.user.domain;

import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.user.domain.entity.AudioConsent;
import web.tosunsaeng.identity.domain.user.domain.entity.User;

@Component
public class UserFactory {

	public static final String GUEST_NICKNAME = "게스트";

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
		// 사용자 엔티티에는 원문 비밀번호가 아닌 해시만 전달한다.
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

	public User createGuest(String installationIdHash, Instant createdAt) {
		Instant requiredCreatedAt = Objects.requireNonNull(
				createdAt,
				"createdAt must not be null"
		);
		AudioConsent audioConsent = AudioConsent.agreed(
				audioPolicyVersion,
				requiredCreatedAt
		);
		return User.createGuest(
				installationIdHash,
				GUEST_NICKNAME,
				audioConsent,
				requiredCreatedAt
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
