package web.tosunsaeng.identity.domain.user.domain;

import java.time.Instant;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;

@Component
public class UserFactory {

	public static final String GUEST_NICKNAME = "게스트";

	private final EmailNormalizer emailNormalizer;
	private final PasswordEncoder passwordEncoder;
	private final ConsentPolicy consentPolicy;

	public UserFactory(
			EmailNormalizer emailNormalizer,
			PasswordEncoder passwordEncoder,
			ConsentPolicy consentPolicy
	) {
		this.emailNormalizer = emailNormalizer;
		this.passwordEncoder = passwordEncoder;
		this.consentPolicy = Objects.requireNonNull(consentPolicy, "consentPolicy must not be null");
	}

	public User create(String email, String rawPassword, String nickname) {
		String normalizedEmail = emailNormalizer.normalize(email);
		// 사용자 엔티티에는 원문 비밀번호가 아닌 해시만 전달한다.
		String passwordHash = passwordEncoder.encode(
				Objects.requireNonNull(rawPassword, "rawPassword must not be null")
		);
		Instant createdAt = Instant.now();
		UserConsents consents = consentPolicy.consentedAt(createdAt);

		return User.create(
				email.trim(),
				normalizedEmail,
				passwordHash,
				nickname,
				consents,
				createdAt
		);
	}

	public User createGuest(String installationIdHash, Instant createdAt) {
		return createGuest(installationIdHash, false, createdAt);
	}

	public User createGuest(
			String installationIdHash,
			boolean qualityReviewConsented,
			Instant createdAt
	) {
		Instant requiredCreatedAt = Objects.requireNonNull(
				createdAt,
				"createdAt must not be null"
		);
		UserConsents consents = consentPolicy.consentedAt(
				qualityReviewConsented,
				requiredCreatedAt
		);
		return User.createGuest(
				installationIdHash,
				GUEST_NICKNAME,
				consents,
				requiredCreatedAt
		);
	}
}
