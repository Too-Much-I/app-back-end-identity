package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.domain.phone.PhoneFingerprint;

@Document(collection = "phone_identities")
@CompoundIndex(
		name = "uk_phone_identities_active_user",
		def = "{ 'userId': 1 }",
		unique = true,
		partialFilter = "{ 'status': 'ACTIVE' }"
)
public class PhoneIdentity {

	@Id
	private String phoneIdentityId;

	private String userId;

	private String fingerprintKeyVersion;

	private String phoneFingerprint;

	private PhoneIdentityStatus status;

	private Instant verifiedAt;

	private Instant createdAt;

	private Instant updatedAt;

	private Instant releasedAt;

	@Version
	private Long version;

	private PhoneIdentity() {
	}

	private PhoneIdentity(
			String phoneIdentityId,
			String userId,
			String fingerprintKeyVersion,
			String phoneFingerprint,
			PhoneIdentityStatus status,
			Instant verifiedAt,
			Instant createdAt,
			Instant updatedAt,
			Instant releasedAt,
			Long version
	) {
		this.phoneIdentityId = requireUuid(phoneIdentityId, "phoneIdentityId");
		this.userId = requireUuid(userId, "userId");
		PhoneFingerprint fingerprint = new PhoneFingerprint(
				fingerprintKeyVersion,
				phoneFingerprint
		);
		this.fingerprintKeyVersion = fingerprint.keyVersion();
		this.phoneFingerprint = fingerprint.value();
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.releasedAt = releasedAt;
		this.version = version;
		validateLifecycle();
	}

	public static PhoneIdentity create(
			String userId,
			PhoneFingerprint activeFingerprint,
			Instant verifiedAt
	) {
		PhoneFingerprint fingerprint = Objects.requireNonNull(
				activeFingerprint,
				"activeFingerprint must not be null"
		);
		Instant requiredVerifiedAt = Objects.requireNonNull(
				verifiedAt,
				"verifiedAt must not be null"
		);
		return new PhoneIdentity(
				UUID.randomUUID().toString(),
				userId,
				fingerprint.keyVersion(),
				fingerprint.value(),
				PhoneIdentityStatus.ACTIVE,
				requiredVerifiedAt,
				requiredVerifiedAt,
				requiredVerifiedAt,
				null,
				null
		);
	}

	public boolean rotateCurrentFingerprint(
			PhoneFingerprint activeFingerprint,
			Instant rotatedAt
	) {
		requireActive();
		PhoneFingerprint fingerprint = Objects.requireNonNull(
				activeFingerprint,
				"activeFingerprint must not be null"
		);
		if (fingerprintKeyVersion.equals(fingerprint.keyVersion())
				&& phoneFingerprint.equals(fingerprint.value())) {
			return false;
		}
		fingerprintKeyVersion = fingerprint.keyVersion();
		phoneFingerprint = fingerprint.value();
		updatedAt = requireNotBefore(rotatedAt, updatedAt, "rotatedAt");
		return true;
	}

	public boolean release(Instant releaseTime) {
		if (status == PhoneIdentityStatus.RELEASED) {
			return false;
		}
		Instant requiredReleaseTime = requireNotBefore(
				releaseTime,
				updatedAt,
				"releaseTime"
		);
		status = PhoneIdentityStatus.RELEASED;
		updatedAt = requiredReleaseTime;
		releasedAt = requiredReleaseTime;
		return true;
	}

	public boolean hasCurrentFingerprint(PhoneFingerprint fingerprint) {
		PhoneFingerprint required = Objects.requireNonNull(
				fingerprint,
				"fingerprint must not be null"
		);
		return fingerprintKeyVersion.equals(required.keyVersion())
				&& phoneFingerprint.equals(required.value());
	}

	private void validateLifecycle() {
		if (status == PhoneIdentityStatus.ACTIVE && releasedAt != null) {
			throw new IllegalArgumentException("Active phone identity must not be released.");
		}
		if (status == PhoneIdentityStatus.RELEASED && releasedAt == null) {
			throw new IllegalArgumentException("Released phone identity requires releasedAt.");
		}
		if (updatedAt.isBefore(createdAt) || verifiedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("Phone identity timestamps are inconsistent.");
		}
	}

	private void requireActive() {
		if (status != PhoneIdentityStatus.ACTIVE) {
			throw new IllegalStateException("Phone identity is not active.");
		}
	}

	private static Instant requireNotBefore(
			Instant value,
			Instant boundary,
			String fieldName
	) {
		Instant required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBefore(boundary)) {
			throw new IllegalArgumentException(fieldName + " must not move backwards");
		}
		return required;
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

	public PhoneIdentityStatus getStatus() {
		return status;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getReleasedAt() {
		return releasedAt;
	}

	public Long getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "PhoneIdentity[phoneIdentityId=" + phoneIdentityId
				+ ", userId=" + userId
				+ ", fingerprintKeyVersion=" + fingerprintKeyVersion
				+ ", phoneFingerprint=[REDACTED], status=" + status + "]";
	}
}
