package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestPrepareRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseGuestPrepareResponse;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

public final class FirebaseGuestPrepareService implements FirebaseGuestPrepareUseCase {
	private SessionSecurityService sessionSecurity;
	@Autowired(required = false)
	public void setSessionSecurity(SessionSecurityService security) { sessionSecurity = security; }

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;
	private final FirebaseAuthenticationVerifier authenticationVerifier;
	private final FirebaseIdentityOwnershipService ownershipService;
	private final FirebaseEnrollmentAttemptService enrollmentAttemptService;
	private final Clock clock;

	public FirebaseGuestPrepareService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository,
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseIdentityOwnershipService ownershipService,
			FirebaseEnrollmentAttemptService enrollmentAttemptService,
			Clock clock
	) {
		this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.authenticationVerifier = Objects.requireNonNull(authenticationVerifier);
		this.ownershipService = Objects.requireNonNull(ownershipService);
		this.enrollmentAttemptService = Objects.requireNonNull(enrollmentAttemptService);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public FirebaseGuestPrepareResponse prepare(FirebaseGuestPrepareRequest request) {
		FirebaseGuestPrepareRequest requiredRequest = Objects.requireNonNull(request);
		String userId = currentUserProvider.getCurrentUserId();
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE || !user.isGuest()) {
			throw new AuthException(AuthErrorStatus.GUEST_UPGRADE_NOT_ALLOWED);
		}
		VerifiedFirebasePrincipal principal = authenticationVerifier.verify(
				requiredRequest.firebaseIdToken(),
				FirebaseVerificationPurpose.GUEST_ENROLLMENT_PREPARE
		);
		FirebaseOwnershipOutcome ownership = ownershipService.resolve(principal, userId);
		if (sessionSecurity != null) sessionSecurity.validateExistingFirebaseProof(principal);
		if (ownership == FirebaseOwnershipOutcome.OWNED_BY_CURRENT_USER) {
			return FirebaseGuestPrepareResponse.alreadyLinked();
		}
		if (ownership == FirebaseOwnershipOutcome.OWNED_BY_OTHER_ACTIVE_MEMBER) {
			return FirebaseGuestPrepareResponse.mergeRequired();
		}
		FirebaseEnrollmentAttempt attempt = enrollmentAttemptService.startOrReuse(
				principal.firebaseProjectId(),
				principal.firebaseUid(),
				FirebaseEnrollmentBindingType.GUEST_USER,
				userId,
				principal.signInMethod()
		);
		long expiresIn = Math.max(
				0,
				Duration.between(clock.instant(), attempt.getExpiresAt()).toMillis()
		);
		return FirebaseGuestPrepareResponse.enrollmentRequired(
				attempt.getEnrollmentId(),
				expiresIn
		);
	}
}
