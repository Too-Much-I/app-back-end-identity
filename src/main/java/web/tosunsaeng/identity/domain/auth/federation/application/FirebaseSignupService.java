package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.session.application.SessionAuthentication;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseSignupRequest;
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
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

public final class FirebaseSignupService implements FirebaseSignupUseCase {

	private final FirebaseAuthenticationVerifier authenticationVerifier;
	private final FirebaseEnrollmentAttemptRepository enrollmentRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final PhoneFingerprintAliasRepository aliasRepository;
	private final PhoneNumberNormalizer phoneNumberNormalizer;
	private final PhoneFingerprintHasher phoneFingerprintHasher;
	private final PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher;
	private final ConsentPolicy consentPolicy;
	private final UserFactory userFactory;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final FirebaseSignupTransactionService transactionService;
	private final AccessTokenIssuer accessTokenIssuer;
	private final Clock clock;
	private final WithdrawalEnrollmentGate withdrawalEnrollmentGate;

	public FirebaseSignupService(
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseEnrollmentAttemptRepository enrollmentRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneNumberNormalizer phoneNumberNormalizer,
			PhoneFingerprintHasher phoneFingerprintHasher,
			PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher,
			ConsentPolicy consentPolicy,
			UserFactory userFactory,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseSignupTransactionService transactionService,
			AccessTokenIssuer accessTokenIssuer,
			Clock clock
	) {
		this(authenticationVerifier, enrollmentRepository, firebaseIdentityRepository,
				socialIdentityRepository, aliasRepository, phoneNumberNormalizer,
				phoneFingerprintHasher, eligibilityFingerprintHasher, consentPolicy,
				userFactory, refreshSessionIssuer, transactionService, accessTokenIssuer,
				clock, null);
	}

	public FirebaseSignupService(
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseEnrollmentAttemptRepository enrollmentRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneNumberNormalizer phoneNumberNormalizer,
			PhoneFingerprintHasher phoneFingerprintHasher,
			PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher,
			ConsentPolicy consentPolicy,
			UserFactory userFactory,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseSignupTransactionService transactionService,
			AccessTokenIssuer accessTokenIssuer,
			Clock clock,
			WithdrawalEnrollmentGate withdrawalEnrollmentGate
	) {
		this.authenticationVerifier = Objects.requireNonNull(authenticationVerifier);
		this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.phoneNumberNormalizer = Objects.requireNonNull(phoneNumberNormalizer);
		this.phoneFingerprintHasher = Objects.requireNonNull(phoneFingerprintHasher);
		this.eligibilityFingerprintHasher = Objects.requireNonNull(eligibilityFingerprintHasher);
		this.consentPolicy = Objects.requireNonNull(consentPolicy);
		this.userFactory = Objects.requireNonNull(userFactory);
		this.refreshSessionIssuer = Objects.requireNonNull(refreshSessionIssuer);
		this.transactionService = Objects.requireNonNull(transactionService);
		this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
		this.clock = Objects.requireNonNull(clock);
		this.withdrawalEnrollmentGate = withdrawalEnrollmentGate;
	}

	@Override
	public FirebaseSignupResponse signup(FirebaseSignupRequest request) {
		FirebaseSignupRequest requiredRequest = Objects.requireNonNull(request);
		VerifiedFirebasePrincipal principal = authenticationVerifier.verify(
				requiredRequest.firebaseIdToken(),
				FirebaseVerificationPurpose.DIRECT_ENROLLMENT
		);
		Instant now = clock.instant();
		FirebaseEnrollmentAttempt attempt = requireActiveAttempt(
				requiredRequest.enrollmentId(),
				principal,
				now
		);
		consentPolicy.validate(
				requiredRequest.isPrivacyConsented(),
				requiredRequest.privacyConsentVersion(),
				requiredRequest.isTermConsented(),
				requiredRequest.termConsentVersion()
		);

		String normalizedPhone = normalizeVerifiedPhone(principal.verifiedPhoneNumber());
		PhoneFingerprintSet phoneFingerprints = phoneFingerprintHasher.fingerprint(normalizedPhone);
		List<PhoneEligibilityFingerprintCandidate> eligibilityCandidates =
				eligibilityFingerprintHasher.fingerprint(normalizedPhone);
		ensureNoExistingOwner(principal, phoneFingerprints);

		User user = userFactory.createFederatedMember(requiredRequest.nickname(), now);
		FirebaseIdentity firebaseIdentity = FirebaseIdentity.create(
				principal.firebaseProjectId(),
				principal.firebaseUid(),
				user.getUserId(),
				now
		);
		List<SocialIdentity> socialIdentities = principal.linkedSocialPrincipals().stream()
				.map(social -> SocialIdentity.create(
						user.getUserId(),
						social.provider(),
						social.providerSubject(),
						now
				))
				.toList();
		PreparedRefreshSession preparedRefreshSession = refreshSessionIssuer.prepare(
				user.getUserId()
		);
		if (refreshSessionIssuer.isFenceEnabled()) {
			preparedRefreshSession.session().attachAuthentication(new SessionAuthentication(
					0, SessionAuthentication.Source.FIREBASE,
					firebaseIdentity.getFirebaseIdentityId(), principal.authTime()));
		}

		IssuedRefreshSession refreshSession;
		try {
			refreshSession = transactionService.register(
					user,
					firebaseIdentity,
					phoneFingerprints,
					eligibilityFingerprintHasher.consumerScopeId(),
					eligibilityCandidates,
					socialIdentities,
					preparedRefreshSession,
					attempt,
					now
			);
		} catch (DuplicateKeyException exception) {
			throw classifyUniqueConflict(principal, phoneFingerprints);
		}

		IssuedAccessToken accessToken = accessTokenIssuer.issue(
				user.getUserId(), user.getAccountType(), Set.of()
		);
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
			VerifiedFirebasePrincipal principal,
			Instant now
	) {
		FirebaseEnrollmentAttempt attempt = enrollmentRepository.findById(enrollmentId)
				.orElseThrow(this::enrollmentConflict);
		if (!attempt.isActiveAt(now)
				|| attempt.getBindingType() != FirebaseEnrollmentBindingType.DIRECT_SIGNUP
				|| attempt.getBoundUserId() != null
				|| !attempt.getFirebaseProjectId().equals(principal.firebaseProjectId())
				|| !attempt.getFirebaseUid().equals(principal.firebaseUid())) {
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

	private void ensureNoExistingOwner(
			VerifiedFirebasePrincipal principal,
			PhoneFingerprintSet phoneFingerprints
	) {
		Optional<FirebaseIdentity> firebaseOwner = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
				principal.firebaseProjectId(),
				principal.firebaseUid()
		);
		if (firebaseOwner.isPresent()) {
			checkOwner(firebaseOwner.orElseThrow().getUserId());
			throw enrollmentConflict();
		}
		for (VerifiedSocialPrincipal social : principal.linkedSocialPrincipals()) {
			Optional<SocialIdentity> socialOwner = socialIdentityRepository
					.findByProviderAndProviderSubject(
					social.provider(),
					social.providerSubject()
			);
			if (socialOwner.isPresent()) {
				checkOwner(socialOwner.orElseThrow().getUserId());
				throw new AuthException(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
			}
		}
		var aliases = aliasRepository.findAllActiveByFingerprints(phoneFingerprints.retained());
		if (!aliases.isEmpty()) {
			aliases.forEach(alias -> checkOwner(alias.getUserId()));
			throw new AuthException(AuthErrorStatus.PHONE_ALREADY_LINKED);
		}
	}

	private AuthException classifyUniqueConflict(
			VerifiedFirebasePrincipal principal,
			PhoneFingerprintSet phoneFingerprints
	) {
		Optional<FirebaseIdentity> firebaseOwner = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
						principal.firebaseProjectId(),
						principal.firebaseUid()
				);
		if (firebaseOwner.isPresent()) {
			checkOwner(firebaseOwner.orElseThrow().getUserId());
			return enrollmentConflict();
		}
		for (VerifiedSocialPrincipal social : principal.linkedSocialPrincipals()) {
			Optional<SocialIdentity> socialOwner = socialIdentityRepository
					.findByProviderAndProviderSubject(
					social.provider(),
					social.providerSubject()
			);
			if (socialOwner.isPresent()) {
				checkOwner(socialOwner.orElseThrow().getUserId());
				return new AuthException(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
			}
		}
		var aliases = aliasRepository.findAllActiveByFingerprints(phoneFingerprints.retained());
		if (!aliases.isEmpty()) {
			aliases.forEach(alias -> checkOwner(alias.getUserId()));
			return new AuthException(AuthErrorStatus.PHONE_ALREADY_LINKED);
		}
		return enrollmentConflict();
	}

	private AuthException enrollmentConflict() {
		return new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);
	}

	private void checkOwner(String ownerUserId) {
		if (withdrawalEnrollmentGate != null) {
			withdrawalEnrollmentGate.checkExistingOwner(ownerUserId);
		}
	}
}
