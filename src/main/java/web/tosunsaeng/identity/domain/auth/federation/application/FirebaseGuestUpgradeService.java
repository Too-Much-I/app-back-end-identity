package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestUpgradeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

public final class FirebaseGuestUpgradeService implements FirebaseGuestUpgradeUseCase {

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;
	private final FirebaseAuthenticationVerifier authenticationVerifier;
	private final FirebaseEnrollmentAttemptRepository enrollmentRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final PhoneFingerprintAliasRepository aliasRepository;
	private final FirebaseIdentityOwnershipService ownershipService;
	private final PhoneNumberNormalizer phoneNumberNormalizer;
	private final PhoneFingerprintHasher phoneFingerprintHasher;
	private final PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher;
	private final ConsentPolicy consentPolicy;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final FirebaseGuestUpgradeTransactionService transactionService;
	private final AccessTokenIssuer accessTokenIssuer;
	private final Clock clock;

	public FirebaseGuestUpgradeService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository,
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseEnrollmentAttemptRepository enrollmentRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			FirebaseIdentityOwnershipService ownershipService,
			PhoneNumberNormalizer phoneNumberNormalizer,
			PhoneFingerprintHasher phoneFingerprintHasher,
			PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher,
			ConsentPolicy consentPolicy,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseGuestUpgradeTransactionService transactionService,
			AccessTokenIssuer accessTokenIssuer,
			Clock clock
	) {
		this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.authenticationVerifier = Objects.requireNonNull(authenticationVerifier);
		this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.ownershipService = Objects.requireNonNull(ownershipService);
		this.phoneNumberNormalizer = Objects.requireNonNull(phoneNumberNormalizer);
		this.phoneFingerprintHasher = Objects.requireNonNull(phoneFingerprintHasher);
		this.eligibilityFingerprintHasher = Objects.requireNonNull(eligibilityFingerprintHasher);
		this.consentPolicy = Objects.requireNonNull(consentPolicy);
		this.refreshSessionIssuer = Objects.requireNonNull(refreshSessionIssuer);
		this.transactionService = Objects.requireNonNull(transactionService);
		this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public FirebaseSignupResponse upgrade(FirebaseGuestUpgradeRequest request) {
		FirebaseGuestUpgradeRequest requiredRequest = Objects.requireNonNull(request);
		String userId = currentUserProvider.getCurrentUserId();
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE || !user.isGuest()) {
			throw new AuthException(AuthErrorStatus.GUEST_UPGRADE_NOT_ALLOWED);
		}
		Instant now = clock.instant();
		FirebaseEnrollmentAttempt attempt = requireActiveAttempt(
				requiredRequest.enrollmentId(),
				userId,
				now
		);
		VerifiedFirebasePrincipal principal = authenticationVerifier.verify(
				requiredRequest.firebaseIdToken(),
				FirebaseVerificationPurpose.GUEST_ENROLLMENT
		);
		if (!attempt.getFirebaseProjectId().equals(principal.firebaseProjectId())
				|| !attempt.getFirebaseUid().equals(principal.firebaseUid())) {
			throw enrollmentConflict();
		}
		consentPolicy.validate(
				requiredRequest.isPrivacyConsented(),
				requiredRequest.privacyConsentVersion(),
				requiredRequest.isTermConsented(),
				requiredRequest.termConsentVersion()
		);
		FirebaseOwnershipOutcome ownership = ownershipService.resolve(principal, userId);
		if (ownership == FirebaseOwnershipOutcome.OWNED_BY_OTHER_ACTIVE_MEMBER) {
			throw new AuthException(AuthErrorStatus.MERGE_REQUIRED);
		}

		String normalizedPhone = normalizeVerifiedPhone(principal.verifiedPhoneNumber());
		PhoneFingerprintSet phoneFingerprints = phoneFingerprintHasher.fingerprint(normalizedPhone);
		List<PhoneEligibilityFingerprintCandidate> eligibilityCandidates =
				eligibilityFingerprintHasher.fingerprint(normalizedPhone);
		ensurePhoneOwner(userId, phoneFingerprints);
		Optional<FirebaseIdentity> currentFirebaseIdentity = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
					principal.firebaseProjectId(),
					principal.firebaseUid()
				);
		FirebaseIdentity firebaseIdentityToCreate = currentFirebaseIdentity.isEmpty()
				? FirebaseIdentity.create(
						principal.firebaseProjectId(),
						principal.firebaseUid(),
						userId,
						now
				)
				: null;
		List<SocialIdentity> socialIdentitiesToCreate = missingSocialIdentities(
				principal,
				userId,
				now
		);
		Instant expectedUpdatedAt = user.getUpdatedAt();
		user.promoteGuestToFederatedMember(
				requiredRequest.nickname(),
				consentPolicy.getPrivacyConsentVersion(),
				consentPolicy.getTermConsentVersion(),
				now
		);
		PreparedRefreshSession preparedRefreshSession = refreshSessionIssuer.prepare(userId);
		IssuedRefreshSession refreshSession;
		try {
			refreshSession = transactionService.upgrade(
					user,
					expectedUpdatedAt,
					firebaseIdentityToCreate,
					phoneFingerprints,
					eligibilityFingerprintHasher.consumerScopeId(),
					eligibilityCandidates,
					socialIdentitiesToCreate,
					preparedRefreshSession,
					attempt,
					now
			);
		} catch (DuplicateKeyException exception) {
			throw classifyUniqueConflict(principal, userId, phoneFingerprints);
		}
		IssuedAccessToken accessToken = accessTokenIssuer.issue(userId, Set.of());
		return new FirebaseSignupResponse(
				accessToken.tokenValue(),
				refreshSession.tokenValue(),
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				Duration.between(accessToken.issuedAt(), accessToken.expiresAt()).toMillis(),
				Duration.between(refreshSession.issuedAt(), refreshSession.expiresAt()).toMillis()
		);
	}

	private FirebaseEnrollmentAttempt requireActiveAttempt(
			String enrollmentId,
			String userId,
			Instant now
	) {
		FirebaseEnrollmentAttempt attempt = enrollmentRepository.findById(enrollmentId)
				.orElseThrow(this::enrollmentConflict);
		if (!attempt.isActiveAt(now)
				|| attempt.getBindingType() != FirebaseEnrollmentBindingType.GUEST_USER
				|| !userId.equals(attempt.getBoundUserId())) {
			throw enrollmentConflict();
		}
		return attempt;
	}

	private String normalizeVerifiedPhone(String verifiedPhoneNumber) {
		if (verifiedPhoneNumber == null) {
			throw new AuthException(AuthErrorStatus.FIREBASE_PHONE_VERIFICATION_REQUIRED);
		}
		try {
			return phoneNumberNormalizer.normalize(verifiedPhoneNumber);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new AuthException(AuthErrorStatus.INVALID_PHONE_NUMBER);
		}
	}

	private void ensurePhoneOwner(String userId, PhoneFingerprintSet fingerprints) {
		if (aliasRepository.findAllActiveByFingerprints(fingerprints.retained()).stream()
				.anyMatch(alias -> !userId.equals(alias.getUserId()))) {
			throw new AuthException(AuthErrorStatus.PHONE_ALREADY_LINKED);
		}
	}

	private List<SocialIdentity> missingSocialIdentities(
			VerifiedFirebasePrincipal principal,
			String userId,
			Instant now
	) {
		List<SocialIdentity> missing = new ArrayList<>();
		for (VerifiedSocialPrincipal social : principal.linkedSocialPrincipals()) {
			Optional<SocialIdentity> existing = socialIdentityRepository
					.findByProviderAndProviderSubject(
						social.provider(),
						social.providerSubject()
					);
			if (existing.isEmpty()) {
				missing.add(SocialIdentity.create(
						userId,
						social.provider(),
						social.providerSubject(),
						now
				));
			} else if (!userId.equals(existing.orElseThrow().getUserId())) {
				throw new AuthException(AuthErrorStatus.MERGE_REQUIRED);
			}
		}
		return List.copyOf(missing);
	}

	private AuthException classifyUniqueConflict(
			VerifiedFirebasePrincipal principal,
			String userId,
			PhoneFingerprintSet phoneFingerprints
	) {
		FirebaseOwnershipOutcome ownership = ownershipService.resolve(principal, userId);
		if (ownership == FirebaseOwnershipOutcome.OWNED_BY_OTHER_ACTIVE_MEMBER) {
			return new AuthException(AuthErrorStatus.MERGE_REQUIRED);
		}
		if (aliasRepository.findAllActiveByFingerprints(phoneFingerprints.retained()).stream()
				.anyMatch(alias -> !userId.equals(alias.getUserId()))) {
			return new AuthException(AuthErrorStatus.PHONE_ALREADY_LINKED);
		}
		return enrollmentConflict();
	}

	private AuthException enrollmentConflict() {
		return new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);
	}
}
