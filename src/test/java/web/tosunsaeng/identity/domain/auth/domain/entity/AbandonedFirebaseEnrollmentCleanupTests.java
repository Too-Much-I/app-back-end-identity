package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentCleanupStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationMethod;

class AbandonedFirebaseEnrollmentCleanupTests {

	private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");

	@Test
	void resumableLifecycleHasGraceWithoutTtlAndAdvancesGeneration() {
		FirebaseEnrollmentAttempt first = attempt(NOW);
		AbandonedFirebaseEnrollmentCleanup lifecycle =
				AbandonedFirebaseEnrollmentCleanup.createResumable(
						first, Duration.ofHours(24), NOW
				);

		assertThat(lifecycle.getStatus())
				.isEqualTo(AbandonedFirebaseEnrollmentCleanupStatus.RESUMABLE);
		assertThat(lifecycle.getGeneration()).isEqualTo(1);
		assertThat(lifecycle.getGraceUntil())
				.isEqualTo(NOW.plus(Duration.ofHours(24).plusMinutes(10)));
		assertThat(lifecycle.getCleanupAt()).isNull();

		FirebaseEnrollmentAttempt second = attempt(NOW.plus(Duration.ofHours(25)));
		lifecycle.resume(second, Duration.ofHours(24), NOW.plus(Duration.ofHours(25)));

		assertThat(lifecycle.getGeneration()).isEqualTo(2);
		assertThat(lifecycle.getSourceEnrollmentId()).isEqualTo(second.getEnrollmentId());
		assertThat(lifecycle.getCleanupAt()).isNull();
	}

	@Test
	void finalizedLifecycleUsesTerminalRetentionAndRejectsInvalidGrace() {
		FirebaseEnrollmentAttempt attempt = attempt(NOW);
		AbandonedFirebaseEnrollmentCleanup finalized =
				AbandonedFirebaseEnrollmentCleanup.createFinalized(
						attempt, NOW.plus(Duration.ofMinutes(1)), Duration.ofDays(7)
				);

		assertThat(finalized.getStatus())
				.isEqualTo(AbandonedFirebaseEnrollmentCleanupStatus.FINALIZED);
		assertThat(finalized.getCleanupAt())
				.isEqualTo(NOW.plus(Duration.ofDays(7).plusMinutes(1)));
		assertThatThrownBy(() -> AbandonedFirebaseEnrollmentCleanup.createResumable(
				attempt, Duration.ZERO, NOW
		)).isInstanceOf(IllegalArgumentException.class);
	}

	private static FirebaseEnrollmentAttempt attempt(Instant createdAt) {
		return FirebaseEnrollmentAttempt.create(
				"test-project", "test-firebase-uid",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP, null,
				FirebaseAuthenticationMethod.GOOGLE, createdAt, Duration.ofMinutes(10)
		);
	}
}
