package web.tosunsaeng.identity.domain.auth.application.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.springframework.dao.DuplicateKeyException;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.repository.FirebaseEnrollmentAttemptRepository;

class FirebaseEnrollmentAttemptServiceTests {

	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "opaque-firebase-uid";
	private static final String GUEST_USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
	private static final Duration ENROLLMENT_TTL = Duration.ofMinutes(10);
	private static final Duration CLEANUP_RETENTION = Duration.ofHours(24);

	@Test
	void reusesActivePendingAttemptForSameBinding() {
		FirebaseEnrollmentAttemptRepository repository = mock(
				FirebaseEnrollmentAttemptRepository.class
		);
		FirebaseEnrollmentAttempt active = attempt(NOW.minusSeconds(30));
		when(repository.findPendingByBinding(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null
		)).thenReturn(Optional.of(active));

		FirebaseEnrollmentAttempt result = service(repository).startOrReuse(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.GOOGLE
		);

		assertThat(result).isSameAs(active);
		verify(repository, never()).save(any(FirebaseEnrollmentAttempt.class));
		verify(repository, never()).expireIfPendingAndExpired(any(), any());
	}

	@Test
	void expiresStalePendingAttemptBeforeCreatingReplacement() {
		FirebaseEnrollmentAttemptRepository repository = mock(
				FirebaseEnrollmentAttemptRepository.class
		);
		FirebaseEnrollmentAttempt expired = attempt(NOW.minus(ENROLLMENT_TTL));
		when(repository.findPendingByBinding(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null
		)).thenReturn(Optional.of(expired), Optional.empty());
		when(repository.expireIfPendingAndExpired(expired.getEnrollmentId(), NOW))
				.thenReturn(true);
		when(repository.save(any(FirebaseEnrollmentAttempt.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		FirebaseEnrollmentAttempt result = service(repository).startOrReuse(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.GOOGLE
		);

		assertThat(result.getEnrollmentId()).isNotEqualTo(expired.getEnrollmentId());
		assertThat(result.getCreatedAt()).isEqualTo(NOW);
		assertThat(result.getExpiresAt()).isEqualTo(NOW.plus(ENROLLMENT_TTL));
		InOrder ordered = inOrder(repository);
		ordered.verify(repository).findPendingByBinding(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null
		);
		ordered.verify(repository).expireIfPendingAndExpired(
				expired.getEnrollmentId(),
				NOW
		);
		ordered.verify(repository).findPendingByBinding(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null
		);
		ordered.verify(repository).save(result);
	}

	@Test
	void duplicateInsertLoserReusesWinnerCreatedByConcurrentRequest() {
		FirebaseEnrollmentAttemptRepository repository = mock(
				FirebaseEnrollmentAttemptRepository.class
		);
		FirebaseEnrollmentAttempt winner = guestAttempt(NOW);
		when(repository.findPendingByBinding(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				GUEST_USER_ID
		)).thenReturn(Optional.empty(), Optional.of(winner));
		when(repository.save(any(FirebaseEnrollmentAttempt.class)))
				.thenThrow(new DuplicateKeyException("concurrent test insert"));

		FirebaseEnrollmentAttempt result = service(repository).startOrReuse(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				GUEST_USER_ID,
				FirebaseAuthenticationMethod.APPLE
		);

		assertThat(result).isSameAs(winner);
		verify(repository).save(any(FirebaseEnrollmentAttempt.class));
		verify(repository, never()).expireIfPendingAndExpired(any(), any());
	}

	@Test
	void consumeDelegatesToAtomicRepositoryTransitionAtApplicationTime() {
		FirebaseEnrollmentAttemptRepository repository = mock(
				FirebaseEnrollmentAttemptRepository.class
		);
		when(repository.consumeIfPendingAndNotExpired(
				"enrollment-id",
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				GUEST_USER_ID,
				NOW
		))
				.thenReturn(true);

		boolean consumed = service(repository).consume(
				"enrollment-id",
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				GUEST_USER_ID
		);

		assertThat(consumed).isTrue();
		verify(repository).consumeIfPendingAndNotExpired(
				"enrollment-id",
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				GUEST_USER_ID,
				NOW
		);
	}

	private FirebaseEnrollmentAttemptService service(
			FirebaseEnrollmentAttemptRepository repository
	) {
		return new FirebaseEnrollmentAttemptService(
				repository,
				Clock.fixed(NOW, ZoneOffset.UTC),
				ENROLLMENT_TTL,
				CLEANUP_RETENTION
		);
	}

	private FirebaseEnrollmentAttempt attempt(Instant createdAt) {
		return FirebaseEnrollmentAttempt.create(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.GOOGLE,
				createdAt,
				ENROLLMENT_TTL,
				CLEANUP_RETENTION
		);
	}

	private FirebaseEnrollmentAttempt guestAttempt(Instant createdAt) {
		return FirebaseEnrollmentAttempt.create(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				GUEST_USER_ID,
				FirebaseAuthenticationMethod.GOOGLE,
				createdAt,
				ENROLLMENT_TTL,
				CLEANUP_RETENTION
		);
	}
}
