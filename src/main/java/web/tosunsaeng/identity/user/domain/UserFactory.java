package web.tosunsaeng.identity.user.domain;

import java.time.Instant;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

	private final EmailNormalizer emailNormalizer;
	private final PasswordEncoder passwordEncoder;

	public UserFactory(EmailNormalizer emailNormalizer, PasswordEncoder passwordEncoder) {
		this.emailNormalizer = emailNormalizer;
		this.passwordEncoder = passwordEncoder;
	}

	public User create(String email, String rawPassword, String nickname) {
		String normalizedEmail = emailNormalizer.normalize(email);
		String passwordHash = passwordEncoder.encode(
				Objects.requireNonNull(rawPassword, "rawPassword must not be null")
		);

		return User.create(email.trim(), normalizedEmail, passwordHash, nickname, Instant.now());
	}
}
