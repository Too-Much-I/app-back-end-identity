package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentCleanupStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;

class FirebaseEnrollmentCoordinationTransactionServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");
	private final FirebaseEnrollmentAttemptRepository attempts = mock(
			FirebaseEnrollmentAttemptRepository.class
	);
	private final AbandonedFirebaseEnrollmentCleanupRepository cleanups = mock(
			AbandonedFirebaseEnrollmentCleanupRepository.class
	);
	private final FirebaseEnrollmentCoordinationTransactionService service =
			new FirebaseEnrollmentCoordinationTransactionService(
					attempts, cleanups, Clock.fixed(NOW, ZoneOffset.UTC),
					Duration.ofMinutes(10), Duration.ofHours(24)
			);

	@Test
	void createsAttemptAndResumableLifecycleTogether() {
		when(cleanups.findByFirebaseProjectIdAndFirebaseUid("project", "uid"))
				.thenReturn(Optional.empty());
		when(attempts.findPendingByBinding(
				"project", "uid", FirebaseEnrollmentBindingType.DIRECT_SIGNUP, null
		)).thenReturn(Optional.empty());
		when(attempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		FirebaseEnrollmentAttempt created = service.startOrReuse(
				"project", "uid", FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null, FirebaseAuthenticationMethod.GOOGLE
		);

		assertThat(created.getCleanupAt()).isNull();
		verify(cleanups).save(any(AbandonedFirebaseEnrollmentCleanup.class));
		verify(attempts).clearCleanupAtForTarget("project", "uid");
	}

	@Test
	void cleanupClaimBlocksFreshEnrollmentWithRestartContract() {
		AbandonedFirebaseEnrollmentCleanup cleanup = mock(
				AbandonedFirebaseEnrollmentCleanup.class
		);
		when(cleanup.getStatus())
				.thenReturn(AbandonedFirebaseEnrollmentCleanupStatus.CLEANUP_IN_PROGRESS);
		when(cleanups.findByFirebaseProjectIdAndFirebaseUid("project", "uid"))
				.thenReturn(Optional.of(cleanup));

		assertThatThrownBy(() -> service.startOrReuse(
				"project", "uid", FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null, FirebaseAuthenticationMethod.GOOGLE
		)).isInstanceOfSatisfying(AuthException.class, exception ->
				assertThat(exception.getErrorCode())
						.isEqualTo(AuthErrorStatus.FIREBASE_ENROLLMENT_RESTART_REQUIRED));
		verify(attempts, never()).save(any());
	}
}
