package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.PhoneRejoinLineageResolver;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

public class FirebaseSignupTransactionService {

	private final UserRepository userRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final PhoneIdentityRepository phoneIdentityRepository;
	private final PhoneFingerprintAliasRepository aliasRepository;
	private final PhoneEligibilityBindingOutboxRepository outboxRepository;
	private final PhoneEligibilityBindingRevisionRepository revisionRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final FirebaseEnrollmentAttemptRepository enrollmentRepository;
	private final FirebaseEnrollmentLifecycleService enrollmentLifecycleService;
	private final PhoneRejoinLineageResolver phoneRejoinLineageResolver;

	@Autowired
	public FirebaseSignupTransactionService(
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneEligibilityBindingOutboxRepository outboxRepository,
			PhoneEligibilityBindingRevisionRepository revisionRepository,
			SocialIdentityRepository socialIdentityRepository,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseEnrollmentAttemptRepository enrollmentRepository
	) {
		this(
				userRepository, firebaseIdentityRepository, phoneIdentityRepository,
				aliasRepository, outboxRepository, revisionRepository,
				socialIdentityRepository, refreshSessionIssuer, enrollmentRepository, null, null
		);
	}

	public FirebaseSignupTransactionService(
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneEligibilityBindingOutboxRepository outboxRepository,
			PhoneEligibilityBindingRevisionRepository revisionRepository,
			SocialIdentityRepository socialIdentityRepository,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseEnrollmentAttemptRepository enrollmentRepository,
			FirebaseEnrollmentLifecycleService enrollmentLifecycleService
	) {
		this(userRepository, firebaseIdentityRepository, phoneIdentityRepository,
				aliasRepository, outboxRepository, revisionRepository,
				socialIdentityRepository, refreshSessionIssuer, enrollmentRepository,
				enrollmentLifecycleService, null);
	}

	public FirebaseSignupTransactionService(
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneEligibilityBindingOutboxRepository outboxRepository,
			PhoneEligibilityBindingRevisionRepository revisionRepository,
			SocialIdentityRepository socialIdentityRepository,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseEnrollmentAttemptRepository enrollmentRepository,
			FirebaseEnrollmentLifecycleService enrollmentLifecycleService,
			PhoneRejoinLineageResolver phoneRejoinLineageResolver
	) {
		this.userRepository = Objects.requireNonNull(userRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.phoneIdentityRepository = Objects.requireNonNull(phoneIdentityRepository);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.outboxRepository = Objects.requireNonNull(outboxRepository);
		this.revisionRepository = Objects.requireNonNull(revisionRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.refreshSessionIssuer = Objects.requireNonNull(refreshSessionIssuer);
		this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository);
		this.enrollmentLifecycleService = enrollmentLifecycleService;
		this.phoneRejoinLineageResolver = phoneRejoinLineageResolver;
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public IssuedRefreshSession register(
			User user,
			FirebaseIdentity firebaseIdentity,
			PhoneFingerprintSet phoneFingerprints,
			String consumerScopeId,
			List<PhoneEligibilityFingerprintCandidate> eligibilityCandidates,
			List<SocialIdentity> socialIdentities,
			PreparedRefreshSession preparedRefreshSession,
			FirebaseEnrollmentAttempt enrollmentAttempt,
			Instant consumedAt
	) {
		if (enrollmentLifecycleService != null) {
			enrollmentLifecycleService.ensureFinalizable(enrollmentAttempt);
		}
		String userId = user.getUserId();
		if (!userId.equals(firebaseIdentity.getUserId())
				|| !userId.equals(preparedRefreshSession.session().getUserId())
				|| socialIdentities.stream().anyMatch(identity -> !userId.equals(identity.getUserId()))) {
			throw new IllegalArgumentException("Firebase signup aggregate user IDs must match.");
		}
		if (!aliasRepository.findAllActiveByFingerprints(phoneFingerprints.retained()).isEmpty()) {
			throw new AuthException(AuthErrorStatus.PHONE_ALREADY_LINKED);
		}

		userRepository.save(user);
		firebaseIdentityRepository.save(firebaseIdentity);
		PhoneIdentity phoneIdentity = phoneIdentityRepository.save(PhoneIdentity.create(
				userId,
				phoneFingerprints.activeWrite(),
				consumedAt
		));
		List<PhoneFingerprintAlias> aliases = phoneFingerprints.retained().stream()
				.map(fingerprint -> PhoneFingerprintAlias.create(
						phoneIdentity.getPhoneIdentityId(),
						userId,
						fingerprint,
						consumedAt
				))
				.toList();
		aliasRepository.saveAll(aliases);
		PhoneEligibilityBindingRevision bindingRevision = revisionRepository.advanceVerified(
				userId,
				consumerScopeId,
				consumedAt
		);
		PhoneEligibilityBindingOutbox outbox = PhoneEligibilityBindingOutbox.createVerified(
				userId,
				consumerScopeId,
				bindingRevision.getRevision(),
				eligibilityCandidates,
				consumedAt,
				consumedAt
		);
		outboxRepository.save(outbox);
		if (phoneRejoinLineageResolver != null) {
			phoneRejoinLineageResolver.resolve(
					phoneFingerprints, consumerScopeId, userId, consumedAt);
		}
		if (!socialIdentities.isEmpty()) {
			socialIdentityRepository.saveAll(socialIdentities);
		}
		IssuedRefreshSession issuedRefreshSession = refreshSessionIssuer.savePrepared(
				preparedRefreshSession
		);
		boolean consumed = enrollmentRepository.consumeIfPendingAndNotExpired(
				enrollmentAttempt.getEnrollmentId(),
				enrollmentAttempt.getFirebaseProjectId(),
				enrollmentAttempt.getFirebaseUid(),
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				consumedAt
		);
		if (!consumed) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);
		}
		if (enrollmentLifecycleService != null) {
			enrollmentLifecycleService.finalizeEnrollment(enrollmentAttempt, consumedAt);
		}
		return issuedRefreshSession;
	}
}
