package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
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
	private final SocialIdentityRepository socialIdentityRepository;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final FirebaseEnrollmentAttemptRepository enrollmentRepository;

	public FirebaseSignupTransactionService(
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneEligibilityBindingOutboxRepository outboxRepository,
			SocialIdentityRepository socialIdentityRepository,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseEnrollmentAttemptRepository enrollmentRepository
	) {
		this.userRepository = Objects.requireNonNull(userRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.phoneIdentityRepository = Objects.requireNonNull(phoneIdentityRepository);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.outboxRepository = Objects.requireNonNull(outboxRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.refreshSessionIssuer = Objects.requireNonNull(refreshSessionIssuer);
		this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public IssuedRefreshSession register(
			User user,
			FirebaseIdentity firebaseIdentity,
			PhoneFingerprintSet phoneFingerprints,
			PhoneEligibilityBindingOutbox outbox,
			List<SocialIdentity> socialIdentities,
			PreparedRefreshSession preparedRefreshSession,
			FirebaseEnrollmentAttempt enrollmentAttempt,
			Instant consumedAt
	) {
		String userId = user.getUserId();
		if (!userId.equals(firebaseIdentity.getUserId())
				|| !userId.equals(outbox.getUserId())
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
		outboxRepository.save(outbox);
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
		return issuedRefreshSession;
	}
}
