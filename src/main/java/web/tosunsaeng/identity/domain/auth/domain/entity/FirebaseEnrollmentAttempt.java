package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationMethod;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentStatus;

@Document(collection = "firebase_enrollment_attempts")
@CompoundIndex(
		name = "uk_firebase_enrollment_pending_binding",
		def = "{ 'firebaseProjectId': 1, 'firebaseUid': 1, "
				+ "'bindingType': 1, 'boundUserId': 1 }",
		unique = true,
		partialFilter = "{ 'status': 'PENDING' }"
)
public class FirebaseEnrollmentAttempt {

	private static final int MAX_PROJECT_ID_LENGTH = 128;
	private static final int MAX_FIREBASE_UID_LENGTH = 128;

	@Id
	private String enrollmentId;

	private String firebaseProjectId;

	private String firebaseUid;

	private FirebaseEnrollmentBindingType bindingType;

	@Field(write = Field.Write.ALWAYS)
	private String boundUserId;

	private FirebaseAuthenticationMethod initialSignInMethod;

	private FirebaseEnrollmentStatus status;

	private Instant expiresAt;

	@Indexed(name = "ttl_firebase_enrollment_cleanup_at", expireAfter = "0s")
	private Instant cleanupAt;

	private Instant createdAt;

	private Instant consumedAt;

	private FirebaseEnrollmentAttempt() {
	}

	private FirebaseEnrollmentAttempt(
			String enrollmentId,
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId,
			FirebaseAuthenticationMethod initialSignInMethod,
			FirebaseEnrollmentStatus status,
			Instant expiresAt,
			Instant cleanupAt,
			Instant createdAt,
			Instant consumedAt
	) {
		this.enrollmentId = requireUuid(enrollmentId, "enrollmentId");
		this.firebaseProjectId = requireOpaque(
				firebaseProjectId,
				"firebaseProjectId",
				MAX_PROJECT_ID_LENGTH
		);
		this.firebaseUid = requireOpaque(
				firebaseUid,
				"firebaseUid",
				MAX_FIREBASE_UID_LENGTH
		);
		this.bindingType = Objects.requireNonNull(bindingType, "bindingType must not be null");
		this.boundUserId = requireBoundUserId(bindingType, boundUserId);
		this.initialSignInMethod = Objects.requireNonNull(
				initialSignInMethod,
				"initialSignInMethod must not be null"
		);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.expiresAt = requireAfter(expiresAt, createdAt, "expiresAt");
		this.cleanupAt = requireAfter(cleanupAt, expiresAt, "cleanupAt");
		this.consumedAt = consumedAt;
	}

	public static FirebaseEnrollmentAttempt create(
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId,
			FirebaseAuthenticationMethod initialSignInMethod,
			Instant createdAt,
			Duration enrollmentTtl,
			Duration cleanupRetention
	) {
		Instant requiredCreatedAt = Objects.requireNonNull(
				createdAt,
				"createdAt must not be null"
		);
		Duration requiredTtl = requirePositive(enrollmentTtl, "enrollmentTtl");
		Duration requiredRetention = requirePositive(
				cleanupRetention,
				"cleanupRetention"
		);
		try {
			Instant expiresAt = requiredCreatedAt.plus(requiredTtl);
			return new FirebaseEnrollmentAttempt(
					UUID.randomUUID().toString(),
					firebaseProjectId,
					firebaseUid,
					bindingType,
					boundUserId,
					initialSignInMethod,
					FirebaseEnrollmentStatus.PENDING,
					expiresAt,
					expiresAt.plus(requiredRetention),
					requiredCreatedAt,
					null
			);
		} catch (DateTimeException | ArithmeticException exception) {
			throw new IllegalArgumentException("Enrollment lifetime is out of range.");
		}
	}

	public boolean isActiveAt(Instant now) {
		return status == FirebaseEnrollmentStatus.PENDING
				&& expiresAt.isAfter(Objects.requireNonNull(now, "now must not be null"));
	}

	public boolean isExpiredAt(Instant now) {
		return !expiresAt.isAfter(Objects.requireNonNull(now, "now must not be null"));
	}

	private static String requireBoundUserId(
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId
	) {
		if (bindingType == FirebaseEnrollmentBindingType.DIRECT_SIGNUP) {
			if (boundUserId != null) {
				throw new IllegalArgumentException("DIRECT_SIGNUP must not have boundUserId");
			}
			return null;
		}
		if (boundUserId == null) {
			throw new IllegalArgumentException("GUEST_USER requires boundUserId");
		}
		return requireUuid(boundUserId, "boundUserId");
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return required;
	}

	private static Instant requireAfter(Instant value, Instant boundary, String fieldName) {
		Instant required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (!required.isAfter(boundary)) {
			throw new IllegalArgumentException(fieldName + " must be after its boundary");
		}
		return required;
	}

	private static String requireOpaque(String value, String fieldName, int maxLength) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank() || !required.equals(required.trim())) {
			throw new IllegalArgumentException(fieldName + " must be a non-blank opaque value");
		}
		if (required.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " is too long");
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

	public String getEnrollmentId() {
		return enrollmentId;
	}

	public String getFirebaseProjectId() {
		return firebaseProjectId;
	}

	public String getFirebaseUid() {
		return firebaseUid;
	}

	public FirebaseEnrollmentBindingType getBindingType() {
		return bindingType;
	}

	public String getBoundUserId() {
		return boundUserId;
	}

	public FirebaseAuthenticationMethod getInitialSignInMethod() {
		return initialSignInMethod;
	}

	public FirebaseEnrollmentStatus getStatus() {
		return status;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getCleanupAt() {
		return cleanupAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}
}
