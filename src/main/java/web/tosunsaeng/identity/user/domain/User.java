package web.tosunsaeng.identity.user.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

	@Id
	private String userId;

	private String email;

	@Indexed(name = "uk_users_normalized_email", unique = true)
	private String normalizedEmail;

	private String passwordHash;

	private String nickname;

	private AudioConsent audioConsent;

	private UserStatus status;

	private Instant createdAt;

	private Instant updatedAt;

	private User() {
	}

	private User(
			String userId,
			String email,
			String normalizedEmail,
			String passwordHash,
			String nickname,
			AudioConsent audioConsent,
			UserStatus status,
			Instant createdAt,
			Instant updatedAt
	) {
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.email = Objects.requireNonNull(email, "email must not be null");
		this.normalizedEmail = Objects.requireNonNull(normalizedEmail, "normalizedEmail must not be null");
		this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
		this.nickname = Objects.requireNonNull(nickname, "nickname must not be null");
		this.audioConsent = Objects.requireNonNull(audioConsent, "audioConsent must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	static User create(
			String email,
			String normalizedEmail,
			String passwordHash,
			String nickname,
			AudioConsent audioConsent,
			Instant createdAt
	) {
		return new User(
				UUID.randomUUID().toString(),
				email,
				normalizedEmail,
				passwordHash,
				nickname,
				audioConsent,
				UserStatus.ACTIVE,
				createdAt,
				createdAt
		);
	}

	public String getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getNormalizedEmail() {
		return normalizedEmail;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getNickname() {
		return nickname;
	}

	public AudioConsent getAudioConsent() {
		return audioConsent;
	}

	public UserStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
