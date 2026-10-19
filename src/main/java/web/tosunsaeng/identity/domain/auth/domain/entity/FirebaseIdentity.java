package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "firebase_identities")
@CompoundIndex(
		name = "uk_firebase_identities_project_uid",
		def = "{ 'firebaseProjectId': 1, 'firebaseUid': 1 }",
		unique = true
)
public class FirebaseIdentity {

	private static final int MAX_PROJECT_ID_LENGTH = 128;
	private static final int MAX_FIREBASE_UID_LENGTH = 128;

	@Id
	private String firebaseIdentityId;

	private String firebaseProjectId;

	private String firebaseUid;

	@Indexed(name = "uk_firebase_identities_user_id", unique = true)
	private String userId;

	private Instant createdAt;

	private FirebaseIdentity() {
	}

	private FirebaseIdentity(
			String firebaseIdentityId,
			String firebaseProjectId,
			String firebaseUid,
			String userId,
			Instant createdAt
	) {
		this.firebaseIdentityId = requireUuid(firebaseIdentityId, "firebaseIdentityId");
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
		this.userId = requireUuid(userId, "userId");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public static FirebaseIdentity create(
			String firebaseProjectId,
			String firebaseUid,
			String userId,
			Instant createdAt
	) {
		return new FirebaseIdentity(
				UUID.randomUUID().toString(),
				firebaseProjectId,
				firebaseUid,
				userId,
				createdAt
		);
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

	public String getFirebaseIdentityId() {
		return firebaseIdentityId;
	}

	public String getFirebaseProjectId() {
		return firebaseProjectId;
	}

	public String getFirebaseUid() {
		return firebaseUid;
	}

	public String getUserId() {
		return userId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
