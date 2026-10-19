package web.tosunsaeng.identity.domain.auth.mergeprogress;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.*;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.*;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.*;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import static web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.*;

@Service
@RequiredArgsConstructor
public class UserMergeQueryService {
	private MeterRegistry metrics;
	@Autowired(required=false)
	public void setMetrics(MeterRegistry metrics) { this.metrics = metrics; }
	private final CurrentUserProvider currentUser;
	private final UserRepository users;
	private final MongoTemplate mongo;
	private final MergeQuerySupport support;
	private final OwnerEventDeliveryRepository deliveries;
	private final OwnerEventCoreRepository cores;
	private final OwnerEventConsumerStateRepository states;
	private final OwnerEventProperties properties;
	private final Clock clock;

	public MergeProgressResponse get(String mergeId) {
		return available(() -> {
			String userId = authorize();
			Instant now = clock.instant();
			support.limit(userId, now);
			MergeQuerySupport.uuid(mergeId);
			var p = mongo.findOne(Query.query(visible(userId, now).and("_id").is(mergeId)), UserMergeProgress.class);
			if (p == null) throw new AuthException(AuthErrorStatus.MERGE_STATUS_NOT_FOUND);
			return render(p, now);
		});
	}
	public Page list(String activeValue, String limitValue, String cursorValue) {
		return available(() -> {
			String userId = authorize();
			Instant now = clock.instant();
			support.limit(userId, now);
			if (!List.of("true", "false").contains(activeValue)) throw MergeQuerySupport.invalid();
			boolean active = Boolean.parseBoolean(activeValue);
			int limit;
			try { limit = Integer.parseInt(limitValue); } catch (RuntimeException e) { throw MergeQuerySupport.invalid(); }
			if (limit < 1 || limit > 50) throw MergeQuerySupport.invalid();
			var criteria = visible(userId, now);
			if (active) criteria.and("completedAt").is(null);
			if (cursorValue != null) {
				var cursor = support.decode(cursorValue, userId, active, now);
				criteria = new Criteria().andOperator(criteria, new Criteria().orOperator(
					Criteria.where("createdAt").lt(cursor.createdAt()),
					Criteria.where("createdAt").is(cursor.createdAt()).and("_id").lt(cursor.mergeId())));
			}
			var rows = mongo.find(Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"))
					.limit(limit + 1), UserMergeProgress.class);
			var selected = rows.stream().limit(limit).toList();
			var results = selected.stream().map(p -> render(p, now)).toList();
			String next = rows.size() > limit ? support.encode(userId, active, selected.getLast(), now) : null;
			return new Page(results, next);
		});
	}
	private Criteria visible(String userId, Instant now) {
		return Criteria.where("targetUserId").is(userId).orOperator(
				Criteria.where("cleanupAt").is(null), Criteria.where("cleanupAt").gt(now));
	}
	private String authorize() {
		String id = currentUser.getCurrentUserId();
		var user = users.findById(id).orElseThrow(() -> new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE));
		if (!user.isMember() || user.getStatus() != UserStatus.ACTIVE || user.getMergedIntoUserId() != null)
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		return id;
	}
	MergeProgressResponse render(UserMergeProgress progress, Instant now) {
		progress.validate();
		var lc = component(progress, OwnerEventConsumer.LEARNING_CORE);
		var billing = component(progress, OwnerEventConsumer.BILLING);
		Status status = progress.getCompletedAt() != null ? Status.COMPLETED
				: lc.status() == ComponentStatus.ACTION_REQUIRED || billing.status() == ComponentStatus.ACTION_REQUIRED
				? Status.ACTION_REQUIRED : Status.PROCESSING;
		Integer poll = status == Status.PROCESSING
				? (Duration.between(progress.getCreatedAt(), now).compareTo(Duration.ofMinutes(1)) < 0 ? 5 : 15) : null;
		if (metrics != null) {
			metrics.counter("identity.merge_progress.query_state", "status", status.name()).increment();
			if (status != Status.COMPLETED) metrics.summary("identity.merge_progress.pending_age_seconds")
					.record(Math.max(0, Duration.between(progress.getCreatedAt(), now).toSeconds()));
		}
		return new MergeProgressResponse(progress.getMergeId(), status, progress.getCreatedAt(),
				progress.getCompletedAt(), lc, billing, poll);
	}
	private Component component(UserMergeProgress progress, OwnerEventConsumer consumer) {
		if (!progress.getRequiredConsumers().contains(consumer)) return new Component(ComponentStatus.NOT_REQUIRED, null);
		Instant confirmed = progress.confirmedAt(consumer);
		if (confirmed != null) return new Component(ComponentStatus.COMPLETED, confirmed);
		var core = cores.findById(progress.getMergeId()).orElseThrow(UserMergeQueryService::unavailable);
		if (core.getEventType() != OwnerEventType.USER_MERGED || core.getProgressContractVersion() != 1 || !core.getRequiredConsumers().equals(progress.getRequiredConsumers())
				|| !core.getTargetUserId().equals(progress.getTargetUserId())
				|| !core.getSourceUserId().equals(progress.getSourceUserId())) throw unavailable();
		var rows = deliveries.findAllByEventId(progress.getMergeId());
		if (rows.size() != progress.getRequiredConsumers().size()
				|| !rows.stream().map(OwnerEventDelivery::getConsumer).collect(Collectors.toSet())
						.equals(progress.getRequiredConsumers())) throw unavailable();
		var own = rows.stream().filter(d -> d.getConsumer() == consumer).findFirst().orElseThrow(UserMergeQueryService::unavailable);
		if (own.getStatus() == null || own.getStatus() == OwnerEventDeliveryStatus.PUBLISHED) throw unavailable();
		var state = states.findById(consumer).orElseThrow(UserMergeQueryService::unavailable);
		if (state.getCircuitStatus() == null || state.getLastPublishedSequence() < 0
                || state.getLastPublishedSequence() >= own.getConsumerSequence()) throw unavailable();
		var head = deliveries.findByConsumerAndConsumerSequence(consumer, state.getLastPublishedSequence() + 1)
				.orElseThrow(UserMergeQueryService::unavailable);
		if (head.getStatus() == null) throw unavailable();
        var headCore = cores.findById(head.getEventId()).orElseThrow(UserMergeQueryService::unavailable);
		boolean blocked = !enabled(consumer, core.getEventType()) || !enabled(consumer, headCore.getEventType())
				|| state.getCircuitStatus() == OwnerEventCircuitStatus.PAUSED
				|| own.getStatus() == OwnerEventDeliveryStatus.DEAD_LETTER
				|| head.getStatus() == OwnerEventDeliveryStatus.DEAD_LETTER;
		return new Component(blocked ? ComponentStatus.ACTION_REQUIRED : ComponentStatus.PENDING, null);
	}
	private boolean enabled(OwnerEventConsumer consumer, OwnerEventType type) {
		if (consumer == OwnerEventConsumer.LEARNING_CORE)
			return type == OwnerEventType.USER_MERGED && properties.isLearningCoreUserMergedPublisherEnabled();
		return type == OwnerEventType.USER_MERGED ? properties.isBillingUserMergedPublisherEnabled()
				: properties.isBillingTrialRebindPublisherEnabled();
	}
	private <T> T available(Supplier<T> action) {
		try { return action.get(); }
		catch (DataAccessException | IllegalStateException e) {
			if (metrics != null) metrics.counter("identity.merge_progress.query_error", "code", "MERGE_STATUS_UNAVAILABLE").increment();
			throw unavailable();
		} catch (AuthException e) {
			if (metrics != null) metrics.counter("identity.merge_progress.query_error", "code", e.getErrorCode().getCode()).increment();
			throw e;
		}
	}
	private static AuthException unavailable() { return new AuthException(AuthErrorStatus.MERGE_STATUS_UNAVAILABLE); }
}
