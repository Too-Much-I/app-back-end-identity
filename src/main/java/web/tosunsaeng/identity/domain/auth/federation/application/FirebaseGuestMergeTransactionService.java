package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import java.util.stream.Stream;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.usermerge.repository.UserMergedOutboxRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventCaptureService;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

public class FirebaseGuestMergeTransactionService {

	private final UserRepository userRepository;
	private final RefreshSessionRepository refreshSessionRepository;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final UserMergedOutboxRepository outboxRepository;
	private final OwnerEventCaptureService ownerEventCaptureService;
	private final OwnerEventProperties ownerEventProperties;

	public FirebaseGuestMergeTransactionService(
			UserRepository userRepository,
			RefreshSessionRepository refreshSessionRepository,
			RefreshSessionIssuer refreshSessionIssuer,
			UserMergedOutboxRepository outboxRepository
	) {
		this(userRepository, refreshSessionRepository, refreshSessionIssuer,
				outboxRepository, null, null);
	}

	public FirebaseGuestMergeTransactionService(
			UserRepository userRepository,
			RefreshSessionRepository refreshSessionRepository,
			RefreshSessionIssuer refreshSessionIssuer,
			UserMergedOutboxRepository outboxRepository,
			OwnerEventCaptureService ownerEventCaptureService,
			OwnerEventProperties ownerEventProperties
	) {
		this.userRepository = Objects.requireNonNull(userRepository);
		this.refreshSessionRepository = Objects.requireNonNull(refreshSessionRepository);
		this.refreshSessionIssuer = Objects.requireNonNull(refreshSessionIssuer);
		this.outboxRepository = Objects.requireNonNull(outboxRepository);
		this.ownerEventCaptureService = ownerEventCaptureService;
		this.ownerEventProperties = ownerEventProperties;
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public IssuedRefreshSession merge(
			User mergedSource,
			Instant expectedSourceUpdatedAt,
			PreparedRefreshSession preparedTargetSession,
			UserMergedOutbox outbox,
			Instant mergedAt
	) {
		User requiredSource = Objects.requireNonNull(mergedSource);
		PreparedRefreshSession requiredTargetSession = Objects.requireNonNull(
				preparedTargetSession
		);
		UserMergedOutbox requiredOutbox = Objects.requireNonNull(outbox);
		Instant requiredMergedAt = Objects.requireNonNull(mergedAt);
		String sourceUserId = requiredSource.getUserId();
		String targetUserId = requiredSource.getMergedIntoUserId();
		if (refreshSessionIssuer.isFenceEnabled()) {
			Long epoch = requiredTargetSession.relatedEpochs().get(sourceUserId);
			if (epoch == null) throw SessionSecurityService.unavailable();
			// Fixed order on the two controls avoids opposite-order merge contention.
			Stream.of(sourceUserId, targetUserId).sorted().forEach(id ->
					refreshSessionIssuer.security().touchExpected(id, id.equals(sourceUserId)
							? epoch : requiredTargetSession.session().getSessionEpoch()));
		}
		if (requiredSource.getStatus() != UserStatus.MERGED
				|| !sourceUserId.equals(requiredOutbox.getSourceUserId())
				|| !targetUserId.equals(requiredOutbox.getTargetUserId())
				|| !targetUserId.equals(requiredTargetSession.session().getUserId())
				|| !requiredMergedAt.equals(requiredOutbox.getOccurredAt())) {
			throw new IllegalArgumentException("Guest merge aggregate user IDs must match.");
		}

		User target = userRepository.findById(targetUserId)
				.orElseThrow(this::mergeConflict);
		if (target.getStatus() != UserStatus.ACTIVE
				|| !target.isMember()
				|| target.getMergedIntoUserId() != null) {
			throw mergeConflict();
		}
		if (!userRepository.mergeGuestIfUnchanged(
				requiredSource,
				Objects.requireNonNull(expectedSourceUpdatedAt)
		)) {
			throw mergeConflict();
		}

		List<RefreshSession> sourceSessions = refreshSessionRepository
				.findAllByUserIdAndRevokedAtIsNull(sourceUserId);
		for (RefreshSession session : sourceSessions) {
			session.mergeGuestAccount(requiredMergedAt);
		}
		if (!sourceSessions.isEmpty()) {
			refreshSessionRepository.saveAll(sourceSessions);
		}
		IssuedRefreshSession issuedRefreshSession = refreshSessionIssuer.savePrepared(
				requiredTargetSession
		);
		if (ownerEventProperties != null && ownerEventProperties.isUserMergedCaptureEnabled()) {
			if (ownerEventCaptureService == null) {
				throw new IllegalStateException("Owner event capture service is unavailable.");
			}
			ownerEventCaptureService.captureUserMerged(
					sourceUserId, targetUserId, requiredMergedAt);
		} else {
			outboxRepository.save(requiredOutbox);
		}
		return issuedRefreshSession;
	}

	private AuthException mergeConflict() {
		return new AuthException(AuthErrorStatus.GUEST_MERGE_CONFLICT);
	}
}
