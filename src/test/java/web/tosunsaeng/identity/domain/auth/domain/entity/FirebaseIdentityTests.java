package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

class FirebaseIdentityTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant CREATED_AT = Instant.parse("2026-08-13T01:02:03Z");

	@Test
	void createsOpaqueFirebaseMappingToCanonicalUuidUser() {
		FirebaseIdentity identity = FirebaseIdentity.create(
				"test-project",
				"Opaque_Firebase_UID",
				USER_ID,
				CREATED_AT
		);

		assertThat(identity.getFirebaseIdentityId()).isNotBlank();
		assertThat(identity.getFirebaseProjectId()).isEqualTo("test-project");
		assertThat(identity.getFirebaseUid()).isEqualTo("Opaque_Firebase_UID");
		assertThat(identity.getUserId()).isEqualTo(USER_ID);
		assertThat(identity.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void rejectsBlankTrimmedOrOversizedOpaqueIdentifiersAndNonUuidUser() {
		assertThatThrownBy(() -> FirebaseIdentity.create(" ", "uid", USER_ID, CREATED_AT))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FirebaseIdentity.create(
				"test-project",
				" uid ",
				USER_ID,
				CREATED_AT
		)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FirebaseIdentity.create(
				"test-project",
				"u".repeat(129),
				USER_ID,
				CREATED_AT
		)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FirebaseIdentity.create(
				"test-project",
				"uid",
				"not-a-uuid",
				CREATED_AT
		)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void declaresProjectUidAndCanonicalUserUniqueIndexes() throws Exception {
		Document document = FirebaseIdentity.class.getAnnotation(Document.class);
		CompoundIndex projectUid = FirebaseIdentity.class.getAnnotation(CompoundIndex.class);
		Indexed userId = FirebaseIdentity.class
				.getDeclaredField("userId")
				.getAnnotation(Indexed.class);

		assertThat(document.collection()).isEqualTo("firebase_identities");
		assertThat(projectUid.name()).isEqualTo("uk_firebase_identities_project_uid");
		assertThat(projectUid.unique()).isTrue();
		assertThat(projectUid.def()).contains("firebaseProjectId", "firebaseUid");
		assertThat(userId.name()).isEqualTo("uk_firebase_identities_user_id");
		assertThat(userId.unique()).isTrue();
	}
}
