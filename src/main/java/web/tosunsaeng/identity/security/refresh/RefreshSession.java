package web.tosunsaeng.identity.security.refresh;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "refresh_sessions")
public class RefreshSession {

	@Id
	private String sessionId;

	private String userId;

	@Indexed(name = "uk_refresh_sessions_token_hash", unique = true)
	private String tokenHash;

	private Instant createdAt;

	@Indexed(name = "ttl_refresh_sessions_expires_at", expireAfter = "0s")
	private Instant expiresAt;

	private Instant lastUsedAt;

	private Instant revokedAt;

	private String rotationFamilyId;

	private String rotatedFromSessionId;

	private String replacedBySessionId;

	private RevocationReason revocationReason;

	@Version
	private Long version;

	private RefreshSession() {
	}

	private RefreshSession(
			String sessionId,
			String userId,
			String tokenHash,
			Instant createdAt,
			Instant expiresAt,
			Instant lastUsedAt,
			Instant revokedAt,
			String rotationFamilyId,
			String rotatedFromSessionId,
			String replacedBySessionId,
			RevocationReason revocationReason
	) {
		this.sessionId = requireUuid(sessionId, "sessionId");
		this.userId = requireUuid(userId, "userId");
		this.tokenHash = requireTokenHash(tokenHash);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.expiresAt = requireExpiresAt(expiresAt, createdAt);
		this.lastUsedAt = Objects.requireNonNull(lastUsedAt, "lastUsedAt must not be null");
		this.revokedAt = revokedAt;
		this.rotationFamilyId = requireUuid(rotationFamilyId, "rotationFamilyId");
		this.rotatedFromSessionId = requireNullableUuid(
				rotatedFromSessionId,
				"rotatedFromSessionId"
		);
		this.replacedBySessionId = requireNullableUuid(
				replacedBySessionId,
				"replacedBySessionId"
		);
		this.revocationReason = revocationReason;
	}

	public static RefreshSession create(
			String userId,
			String tokenHash,
			Instant createdAt,
			Instant expiresAt
	) {
		return new RefreshSession(
				newSessionId(),
				userId,
				tokenHash,
				createdAt,
				expiresAt,
				createdAt,
				null,
				UUID.randomUUID().toString(),
				null,
				null,
				null
		);
	}

	public static RefreshSession createRotated(
			String sessionId,
			String userId,
			String rotationFamilyId,
			String rotatedFromSessionId,
			String tokenHash,
			Instant createdAt,
			Instant expiresAt
	) {
		return new RefreshSession(
				sessionId,
				userId,
				tokenHash,
				createdAt,
				expiresAt,
				createdAt,
				null,
				rotationFamilyId,
				rotatedFromSessionId,
				null,
				null
		);
	}

	public static String newSessionId() {
		return UUID.randomUUID().toString();
	}

	public String initializeRotationFamilyIfMissing() {
		if (rotationFamilyId == null) {
			rotationFamilyId = UUID.randomUUID().toString();
		}
		return rotationFamilyId;
	}

	public void rotate(Instant revokedAt, String replacementSessionId) {
		revoke(revokedAt, RevocationReason.ROTATED, replacementSessionId);
	}

	public void logout(Instant revokedAt) {
		revoke(revokedAt, RevocationReason.LOGOUT, null);
	}

	public void revokeForReuse(Instant revokedAt) {
		revoke(revokedAt, RevocationReason.REUSE_DETECTED, null);
	}

	public boolean isRevoked() {
		return revokedAt != null || revocationReason != null;
	}

	public boolean isExpiredAt(Instant currentTime) {
		return !expiresAt.isAfter(Objects.requireNonNull(
				currentTime,
				"currentTime must not be null"
		));
	}

	private void revoke(
			Instant revocationTime,
			RevocationReason reason,
			String replacementSessionId
	) {
		if (isRevoked()) {
			throw new IllegalStateException("RefreshSession is already revoked.");
		}
		Instant requiredRevocationTime = Objects.requireNonNull(
				revocationTime,
				"revocationTime must not be null"
		);
		this.lastUsedAt = requiredRevocationTime;
		this.revokedAt = requiredRevocationTime;
		this.revocationReason = Objects.requireNonNull(reason, "reason must not be null");
		this.replacedBySessionId = requireNullableUuid(
				replacementSessionId,
				"replacedBySessionId"
		);
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equalsIgnoreCase(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(fieldName + " must be a UUID.");
		}
	}

	private static String requireTokenHash(String tokenHash) {
		String requiredTokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
		if (requiredTokenHash.isBlank()) {
			throw new IllegalArgumentException("tokenHash must not be blank");
		}
		return requiredTokenHash;
	}

	private static String requireNullableUuid(String value, String fieldName) {
		return value == null ? null : requireUuid(value, fieldName);
	}

	private static Instant requireExpiresAt(Instant expiresAt, Instant createdAt) {
		Instant requiredExpiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		if (!requiredExpiresAt.isAfter(createdAt)) {
			throw new IllegalArgumentException("expiresAt must be after createdAt");
		}
		return requiredExpiresAt;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getUserId() {
		return userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public String getRotationFamilyId() {
		return rotationFamilyId;
	}

	public String getRotatedFromSessionId() {
		return rotatedFromSessionId;
	}

	public String getReplacedBySessionId() {
		return replacedBySessionId;
	}

	public RevocationReason getRevocationReason() {
		return revocationReason;
	}

	public Long getVersion() {
		return version;
	}
}
