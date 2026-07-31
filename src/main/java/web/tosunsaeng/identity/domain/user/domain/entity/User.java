package web.tosunsaeng.identity.domain.user.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

@Document(collection = "users")
public class User {

	@Id
	private String userId;

	private String email;

	@Indexed(
			name = "uk_users_normalized_email_present",
			unique = true,
			partialFilter = "{ 'normalizedEmail': { '$type': 'string' } }"
	)
	private String normalizedEmail;

	private String passwordHash;

	@Indexed(
			name = "uk_users_guest_installation_id_hash",
			unique = true,
			partialFilter = "{ 'guestInstallationIdHash': { '$type': 'string' } }"
	)
	private String guestInstallationIdHash;

	private String nickname;

	private UserProvider provider;

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
			String guestInstallationIdHash,
			String nickname,
			UserProvider provider,
			AudioConsent audioConsent,
			UserStatus status,
			Instant createdAt,
			Instant updatedAt
	) {
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.email = email;
		this.normalizedEmail = normalizedEmail;
		this.passwordHash = passwordHash;
		this.guestInstallationIdHash = guestInstallationIdHash;
		this.nickname = Objects.requireNonNull(nickname, "nickname must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		validateProviderFields();
		this.audioConsent = Objects.requireNonNull(audioConsent, "audioConsent must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public static User create(
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
				null,
				nickname,
				UserProvider.LOCAL,
				audioConsent,
				UserStatus.ACTIVE,
				createdAt,
				createdAt
		);
	}

	public static User createGuest(
			String guestInstallationIdHash,
			String nickname,
			AudioConsent audioConsent,
			Instant createdAt
	) {
		return new User(
				UUID.randomUUID().toString(),
				null,
				null,
				null,
				guestInstallationIdHash,
				nickname,
				UserProvider.GUEST,
				audioConsent,
				UserStatus.ACTIVE,
				createdAt,
				createdAt
		);
	}

	private void validateProviderFields() {
		if (provider == UserProvider.GUEST) {
			if (email != null || normalizedEmail != null || passwordHash != null) {
				throw new IllegalArgumentException("Guest credentials must be absent.");
			}
			requireGuestInstallationIdHash(guestInstallationIdHash);
			return;
		}
		Objects.requireNonNull(email, "email must not be null");
		Objects.requireNonNull(normalizedEmail, "normalizedEmail must not be null");
		Objects.requireNonNull(passwordHash, "passwordHash must not be null");
	}

	private static String requireGuestInstallationIdHash(String hash) {
		String requiredHash = Objects.requireNonNull(
				hash,
				"guestInstallationIdHash must not be null"
		);
		if (!requiredHash.matches("[A-Za-z0-9_-]{43}")) {
			throw new IllegalArgumentException("Guest installation hash has an invalid format.");
		}
		return requiredHash;
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

	public String getGuestInstallationIdHash() {
		return guestInstallationIdHash;
	}

	public String getNickname() {
		return nickname;
	}

	public UserProvider getProvider() {
		return provider == null ? UserProvider.LOCAL : provider;
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
