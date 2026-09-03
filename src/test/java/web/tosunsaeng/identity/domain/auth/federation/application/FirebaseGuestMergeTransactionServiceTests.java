package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.usermerge.repository.UserMergedOutboxRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventCaptureService;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class FirebaseGuestMergeTransactionServiceTests {

	private static final Instant CREATED_AT = Instant.parse("2026-08-20T01:00:00Z");
	private static final Instant MERGED_AT = Instant.parse("2026-08-20T02:00:00Z");
	private final UserRepository userRepository = mock(UserRepository.class);
	private final RefreshSessionRepository sessionRepository = mock(
			RefreshSessionRepository.class
	);
	private final RefreshSessionIssuer sessionIssuer = mock(RefreshSessionIssuer.class);
	private final UserMergedOutboxRepository outboxRepository = mock(
			UserMergedOutboxRepository.class
	);
	private FirebaseGuestMergeTransactionService service;

	@BeforeEach
	void setUp() {
		service = new FirebaseGuestMergeTransactionService(
				userRepository,
				sessionRepository,
				sessionIssuer,
				outboxRepository
		);
	}

	@Test
	void atomicallyMergesSourceRevokesSessionsAndCreatesTargetSessionAndOutbox() {
		Aggregate aggregate = aggregate();
		RefreshSession sourceSession = RefreshSession.create(
				aggregate.source().getUserId(),
				"source-hash",
				CREATED_AT,
				MERGED_AT.plusSeconds(3600)
		);
		IssuedRefreshSession issued = new IssuedRefreshSession(
				"target-refresh",
				MERGED_AT,
				MERGED_AT.plusSeconds(3600)
		);
		when(userRepository.findById(aggregate.target().getUserId()))
				.thenReturn(Optional.of(aggregate.target()));
		when(userRepository.mergeGuestIfUnchanged(
				aggregate.mergedSource(),
				aggregate.source().getUpdatedAt()
		)).thenReturn(true);
		when(sessionRepository.findAllByUserIdAndRevokedAtIsNull(
				aggregate.source().getUserId()
		)).thenReturn(List.of(sourceSession));
		when(sessionIssuer.savePrepared(aggregate.prepared())).thenReturn(issued);

		assertThat(service.merge(
				aggregate.mergedSource(),
				aggregate.source().getUpdatedAt(),
				aggregate.prepared(),
				aggregate.outbox(),
				MERGED_AT
		)).isSameAs(issued);
		assertThat(sourceSession.getRevocationReason())
				.isEqualTo(RevocationReason.GUEST_MERGED);
		assertThat(sourceSession.getRevokedAt()).isEqualTo(MERGED_AT);
		verify(sessionRepository).saveAll(List.of(sourceSession));
		verify(sessionIssuer).savePrepared(aggregate.prepared());
		verify(outboxRepository).save(aggregate.outbox());
	}

	@Test
	void concurrencyConflictStopsEveryFollowingMutation() {
		Aggregate aggregate = aggregate();
		when(userRepository.findById(aggregate.target().getUserId()))
				.thenReturn(Optional.of(aggregate.target()));
		when(userRepository.mergeGuestIfUnchanged(any(), any())).thenReturn(false);

		AuthException exception = catchThrowableOfType(
				AuthException.class,
				() -> service.merge(
						aggregate.mergedSource(),
						aggregate.source().getUpdatedAt(),
						aggregate.prepared(),
						aggregate.outbox(),
						MERGED_AT
				)
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.GUEST_MERGE_CONFLICT);
		verify(sessionRepository, never()).findAllByUserIdAndRevokedAtIsNull(any());
		verify(sessionIssuer, never()).savePrepared(any());
		verify(outboxRepository, never()).save(any());
	}

	@Test
	void newCaptureWritesDurableFanoutInsteadOfLegacyOutbox() {
		OwnerEventCaptureService captureService = mock(OwnerEventCaptureService.class);
		OwnerEventProperties properties = new OwnerEventProperties();
		properties.setUserMergedCaptureEnabled(true);
		service = new FirebaseGuestMergeTransactionService(
				userRepository, sessionRepository, sessionIssuer, outboxRepository,
				captureService, properties);
		Aggregate aggregate = aggregate();
		IssuedRefreshSession issued = new IssuedRefreshSession(
				"target-refresh", MERGED_AT, MERGED_AT.plusSeconds(3600));
		when(userRepository.findById(aggregate.target().getUserId()))
				.thenReturn(Optional.of(aggregate.target()));
		when(userRepository.mergeGuestIfUnchanged(any(), any())).thenReturn(true);
		when(sessionRepository.findAllByUserIdAndRevokedAtIsNull(any())).thenReturn(List.of());
		when(sessionIssuer.savePrepared(aggregate.prepared())).thenReturn(issued);

		service.merge(aggregate.mergedSource(), aggregate.source().getUpdatedAt(),
				aggregate.prepared(), aggregate.outbox(), MERGED_AT);

		verify(captureService).captureUserMerged(
				aggregate.source().getUserId(), aggregate.target().getUserId(), MERGED_AT);
		verify(outboxRepository, never()).save(any());
	}

	private Aggregate aggregate() {
		User source = User.createGuest(
				"ccccccccccccccccccccccccccccccccccccccccccc",
				"게스트",
				UserConsents.unconsented(),
				CREATED_AT
		);
		User target = User.createFederatedMember(
				"회원",
				UserConsents.unconsented(),
				CREATED_AT
		);
		User mergedSource = source.toMergedTombstone(target.getUserId(), MERGED_AT);
		RefreshSession targetSession = RefreshSession.create(
				target.getUserId(),
				"target-hash",
				MERGED_AT,
				MERGED_AT.plusSeconds(3600)
		);
		PreparedRefreshSession prepared = new PreparedRefreshSession(
				"target-refresh",
				targetSession
		);
		return new Aggregate(
				source,
				target,
				mergedSource,
				prepared,
				UserMergedOutbox.create(source.getUserId(), target.getUserId(), MERGED_AT)
		);
	}

	private record Aggregate(
			User source,
			User target,
			User mergedSource,
			PreparedRefreshSession prepared,
			UserMergedOutbox outbox
	) {
	}
}
