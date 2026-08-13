package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import web.tosunsaeng.identity.domain.auth.application.firebase.FirebaseAuthenticationMethod;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentStatus;

class FirebaseEnrollmentAttemptTests {

	private static final String GUEST_USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant CREATED_AT = Instant.parse("2026-08-13T01:02:03Z");

	@Test
	void directSignupHasNullBindingAndIndependentCleanupTime() {
		FirebaseEnrollmentAttempt attempt = attempt(
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null
		);

		assertThat(attempt.getStatus()).isEqualTo(FirebaseEnrollmentStatus.PENDING);
		assertThat(attempt.getBoundUserId()).isNull();
		assertThat(attempt.getInitialSignInMethod())
				.isEqualTo(FirebaseAuthenticationMethod.GOOGLE);
		assertThat(attempt.getExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofMinutes(10)));
		assertThat(attempt.getCleanupAt()).isEqualTo(
				CREATED_AT.plus(Duration.ofMinutes(10)).plus(Duration.ofHours(24))
		);
		assertThat(attempt.isActiveAt(attempt.getExpiresAt().minusNanos(1))).isTrue();
		assertThat(attempt.isActiveAt(attempt.getExpiresAt())).isFalse();
		assertThat(attempt.isExpiredAt(attempt.getExpiresAt())).isTrue();
	}

	@Test
	void guestBindingRequiresCanonicalUuidAndDirectBindingRejectsUser() {
		assertThat(attempt(FirebaseEnrollmentBindingType.GUEST_USER, GUEST_USER_ID)
				.getBoundUserId()).isEqualTo(GUEST_USER_ID);
		assertThatThrownBy(() -> attempt(FirebaseEnrollmentBindingType.GUEST_USER, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("GUEST_USER requires boundUserId");
		assertThatThrownBy(() -> attempt(
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				GUEST_USER_ID
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("DIRECT_SIGNUP must not have boundUserId");
	}

	@Test
	void declaresPendingPartialUniqueAndSeparateCleanupTtlIndexes() throws Exception {
		Document document = FirebaseEnrollmentAttempt.class.getAnnotation(Document.class);
		CompoundIndex pendingBinding = FirebaseEnrollmentAttempt.class.getAnnotation(
				CompoundIndex.class
		);
		Field boundUserId = FirebaseEnrollmentAttempt.class
				.getDeclaredField("boundUserId")
				.getAnnotation(Field.class);
		Indexed cleanupAt = FirebaseEnrollmentAttempt.class
				.getDeclaredField("cleanupAt")
				.getAnnotation(Indexed.class);

		assertThat(document.collection()).isEqualTo("firebase_enrollment_attempts");
		assertThat(pendingBinding.name())
				.isEqualTo("uk_firebase_enrollment_pending_binding");
		assertThat(pendingBinding.unique()).isTrue();
		assertThat(pendingBinding.def()).contains(
				"firebaseProjectId",
				"firebaseUid",
				"bindingType",
				"boundUserId"
		);
		assertThat(pendingBinding.partialFilter()).contains("status", "PENDING");
		assertThat(boundUserId.write()).isEqualTo(Field.Write.ALWAYS);
		assertThat(cleanupAt.name()).isEqualTo("ttl_firebase_enrollment_cleanup_at");
		assertThat(cleanupAt.expireAfter()).isEqualTo("0s");
	}

	private FirebaseEnrollmentAttempt attempt(
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId
	) {
		return FirebaseEnrollmentAttempt.create(
				"test-project",
				"Opaque_Firebase_UID",
				bindingType,
				boundUserId,
				FirebaseAuthenticationMethod.GOOGLE,
				CREATED_AT,
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}
}
