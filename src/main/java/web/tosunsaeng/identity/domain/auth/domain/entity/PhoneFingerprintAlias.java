package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;

@Document(collection = "phone_fingerprint_aliases")
@CompoundIndexes({
		@CompoundIndex(
				name = "uk_phone_aliases_active_fingerprint",
				def = "{ 'fingerprintKeyVersion': 1, 'phoneFingerprint': 1 }",
				unique = true,
				partialFilter = "{ 'status': 'ACTIVE' }"
		),
		@CompoundIndex(
				name = "uk_phone_aliases_active_identity_version",
				def = "{ 'phoneIdentityId': 1, 'fingerprintKeyVersion': 1 }",
				unique = true,
				partialFilter = "{ 'status': 'ACTIVE' }"
		),
		@CompoundIndex(
				name = "ix_phone_aliases_user_status",
				def = "{ 'userId': 1, 'status': 1 }"
		)
})
public class PhoneFingerprintAlias {

	@Id
	private String aliasId;

	private String phoneIdentityId;

	private String userId;

	private String fingerprintKeyVersion;

	private String phoneFingerprint;

	private PhoneFingerprintAliasStatus status;

	private Instant createdAt;

	private Instant releasedAt;

	private PhoneFingerprintAlias() {
	}

	private PhoneFingerprintAlias(
			String aliasId,
			String phoneIdentityId,
			String userId,
			String fingerprintKeyVersion,
			String phoneFingerprint,
			PhoneFingerprintAliasStatus status,
			Instant createdAt,
			Instant releasedAt
	) {
		this.aliasId = requireUuid(aliasId, "aliasId");
		this.phoneIdentityId = requireUuid(phoneIdentityId, "phoneIdentityId");
		this.userId = requireUuid(userId, "userId");
		PhoneFingerprint fingerprint = new PhoneFingerprint(
				fingerprintKeyVersion,
				phoneFingerprint
		);
		this.fingerprintKeyVersion = fingerprint.keyVersion();
		this.phoneFingerprint = fingerprint.value();
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.releasedAt = releasedAt;
		validateLifecycle();
	}

	public static PhoneFingerprintAlias create(
			String phoneIdentityId,
			String userId,
			PhoneFingerprint fingerprint,
			Instant createdAt
	) {
		PhoneFingerprint required = Objects.requireNonNull(
				fingerprint,
				"fingerprint must not be null"
		);
		return new PhoneFingerprintAlias(
				UUID.randomUUID().toString(),
				phoneIdentityId,
				userId,
				required.keyVersion(),
				required.value(),
				PhoneFingerprintAliasStatus.ACTIVE,
				createdAt,
				null
		);
	}

	public boolean release(Instant releaseTime) {
		if (status == PhoneFingerprintAliasStatus.RELEASED) {
			return false;
		}
		Instant required = Objects.requireNonNull(
				releaseTime,
				"releaseTime must not be null"
		);
		if (required.isBefore(createdAt)) {
			throw new IllegalArgumentException("releaseTime must not precede createdAt");
		}
		status = PhoneFingerprintAliasStatus.RELEASED;
		releasedAt = required;
		return true;
	}

	public boolean matches(PhoneFingerprint fingerprint) {
		PhoneFingerprint required = Objects.requireNonNull(
				fingerprint,
				"fingerprint must not be null"
		);
		return fingerprintKeyVersion.equals(required.keyVersion())
				&& phoneFingerprint.equals(required.value());
	}

	private void validateLifecycle() {
		if (status == PhoneFingerprintAliasStatus.ACTIVE && releasedAt != null) {
			throw new IllegalArgumentException("Active phone alias must not be released.");
		}
		if (status == PhoneFingerprintAliasStatus.RELEASED && releasedAt == null) {
			throw new IllegalArgumentException("Released phone alias requires releasedAt.");
		}
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

	public String getAliasId() {
		return aliasId;
	}

	public String getPhoneIdentityId() {
		return phoneIdentityId;
	}

	public String getUserId() {
		return userId;
	}

	public String getFingerprintKeyVersion() {
		return fingerprintKeyVersion;
	}

	public String getPhoneFingerprint() {
		return phoneFingerprint;
	}

	public PhoneFingerprintAliasStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReleasedAt() {
		return releasedAt;
	}

	@Override
	public String toString() {
		return "PhoneFingerprintAlias[aliasId=" + aliasId
				+ ", phoneIdentityId=" + phoneIdentityId
				+ ", userId=" + userId
				+ ", fingerprintKeyVersion=" + fingerprintKeyVersion
				+ ", phoneFingerprint=[REDACTED], status=" + status + "]";
	}
}
