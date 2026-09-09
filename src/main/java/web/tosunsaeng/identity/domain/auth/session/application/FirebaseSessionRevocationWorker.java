package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.domain.LogoutAllOperation;
import web.tosunsaeng.identity.domain.auth.session.domain.LogoutAllOperation.Status;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.SessionRevocationProperties;

public class FirebaseSessionRevocationWorker {
	private final SessionSecurityService security;
	private final MongoTemplate mongo;
	private final FirebaseIdentityRepository identities;
	private final FirebaseSessionRevocationPort port;
	private final SessionRevocationProperties properties;
	private final LogoutAllCoordinator coordinator;
	private final Clock clock;
	private MeterRegistry metrics;
	@Autowired(required = false)
	public void setMetrics(MeterRegistry metrics) { this.metrics = metrics; }
	private void record(String outcome) {
		if (metrics == null) return;
		Runnable increment = () -> metrics.counter("identity.session.revocation", "outcome", outcome).increment();
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override public void afterCommit() { increment.run(); }
			});
		} else increment.run();
	}

	public FirebaseSessionRevocationWorker(SessionSecurityService security, MongoTemplate mongo,
			FirebaseIdentityRepository identities, FirebaseSessionRevocationPort port,
			SessionRevocationProperties properties, LogoutAllCoordinator coordinator, Clock clock) {
		this.security = security; this.mongo = mongo; this.identities = identities;
		this.port = port; this.properties = properties; this.coordinator = coordinator; this.clock = clock;
	}

	public void runBatch() {
		Instant now = clock.instant();
		security.markInvalidSessionsBatch(now);
		var candidates = mongo.find(Query.query(Criteria.where("status").in(Status.PENDING, Status.CLAIMED,
				Status.REVOKING, Status.VERIFYING, Status.RETRY_WAIT).and("nextAttemptAt").lte(now)
				.orOperator(Criteria.where("leaseExpiresAt").is(null), Criteria.where("leaseExpiresAt").lte(now)))
				.with(Sort.by("requestedAt", "epoch")).limit(properties.getBatchSize()), LogoutAllOperation.class);
		for (var candidate : candidates) {
			try { process(candidate.getLogoutId()); }
			catch (RuntimeException ignored) {
				// Persisted lease/mutation evidence is authoritative. Never log provider payloads.
				org.slf4j.LoggerFactory.getLogger(getClass()).warn("Session revocation worker requires retry or reconciliation.");
			}
		}
	}

	public void process(String logoutId) {
		String owner = UUID.randomUUID().toString();
		LogoutAllOperation claimed = security.transaction(() -> {
			var op = mongo.findById(logoutId, LogoutAllOperation.class);
			Instant now = clock.instant();
			if (op == null || op.terminal() || op.getStatus() == Status.RECONCILIATION_REQUIRED
					|| op.getNextAttemptAt() == null || op.getNextAttemptAt().isAfter(now)) return null;
			if (op.getLeaseExpiresAt() != null && op.getLeaseExpiresAt().isAfter(now)) return null;
			var control = security.control(op.getUserId());
			if (control.getActiveLogoutId() != null && !control.getActiveLogoutId().equals(logoutId)) {
				deferQueueCheck(op, now); return null;
			}
			// Do not overtake an earlier queued operation if the slot was just released.
			if (mongo.exists(Query.query(Criteria.where("userId").is(op.getUserId()).and("epoch").lt(op.getEpoch())
					.and("status").nin(Status.COMPLETED, Status.SUPERSEDED_BY_WITHDRAWAL)), LogoutAllOperation.class)) {
				deferQueueCheck(op, now); return null;
			}
			control.claimLogout(logoutId); mongo.save(control);
			if (op.isMutationStarted() && !op.isDispatchAcknowledged()) {
				op.unknown("UNRESOLVED_DISPATCH"); mongo.save(op); record("unknown"); return null;
			}
			op.claim(owner, now.plus(properties.getLease())); mongo.save(op); return op;
		});
		if (claimed == null) return;
		try {
			validateTarget(claimed);
			var snapshot = port.inspect(target(claimed));
			validateSnapshot(claimed, snapshot);
			if (!claimed.isMutationStarted()) {
				claimed = start(logoutId, owner, snapshot.createdAt());
				if (claimed == null) return;
				try { port.revoke(target(claimed), claimed.getDispatchAt()); }
				catch (RuntimeException exception) {
					// A lost response is not proof of a failed mutation. Apply observed protection,
					// but keep the unresolved actor and do not automatically dispatch again.
					try { applyEvidence(logoutId, owner, port.inspect(target(claimed)), false); }
					catch (RuntimeException ignored) { }
					fail(logoutId, owner, "RESULT_UNKNOWN", false); return;
				}
				security.transaction(() -> {
					var current = owned(logoutId, owner);
					if (current != null) { current.acknowledge(); mongo.save(current); }
					return null;
				});
				snapshot = port.inspect(target(claimed));
			}
			applyEvidence(logoutId, owner, snapshot, true);
		} catch (FirebaseSessionRevocationPort.Failure failure) {
			fail(logoutId, owner, failure.kind().name(), failure.kind() == FirebaseSessionRevocationPort.Failure.Kind.READ_TRANSIENT);
		}
	}

	private LogoutAllOperation start(String id, String owner, Instant remoteCreation) {
		return security.transaction(() -> {
			var op = owned(id, owner);
			if (op == null || !op.getLeaseExpiresAt().isAfter(clock.instant())) return null;
			security.requireActive(op.getUserId()); validateTarget(op);
			var control = security.control(op.getUserId());
			if (!id.equals(control.getActiveLogoutId())) return null;
			control.touch(); mongo.save(control);
			op.startMutation(remoteCreation, clock.instant()); mongo.save(op); return op;
		});
	}

	private void applyEvidence(String id, String owner, FirebaseSessionRevocationPort.Snapshot snapshot, boolean finish) {
		security.transaction(() -> {
			var op = owned(id, owner);
			if (op == null) return null;
			validateTarget(op); validateSnapshot(op, snapshot);
			if (snapshot.validAfter().isBefore(op.getDispatchAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))) {
				throw new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.READ_TRANSIENT);
			}
			var control = security.control(op.getUserId());
			if (!id.equals(control.getActiveLogoutId())) throw new IllegalStateException("Logout slot changed.");
			control.confirmRevocation(op.getBindingId(), snapshot.validAfter()); op.observed(snapshot.validAfter());
			if (finish && op.isDispatchAcknowledged()) {
				Instant completedAt = clock.instant();
				op.complete(completedAt, coordinator.cleanupAt(op, completedAt)); control.releaseLogout(id);
				record("completed");
			}
			mongo.save(control); mongo.save(op); return null;
		});
	}

	private void fail(String id, String owner, String code, boolean safeRetry) {
		security.transaction(() -> {
			var op = owned(id, owner);
			if (op == null) return null;
			if (safeRetry && (!op.isMutationStarted() || op.isDispatchAcknowledged())
					&& op.getAttemptCount() + 1 < properties.getMaxAttempts()) {
				long cap = Math.min(300, 5L << Math.min(op.getAttemptCount(), 6));
				op.retry(code, clock.instant().plusSeconds(ThreadLocalRandom.current().nextLong(1, cap + 1)));
				record("retry");
			} else { op.unknown(code); record("reconciliation"); }
			mongo.save(op); return null;
		});
	}
	private LogoutAllOperation owned(String id, String owner) {
		var op = mongo.findById(id, LogoutAllOperation.class);
		return op != null && !op.terminal() && op.getStatus() != Status.RECONCILIATION_REQUIRED
				&& Objects.equals(owner, op.getLeaseOwner()) ? op : null;
	}
	private void deferQueueCheck(LogoutAllOperation operation, Instant now) {
		// A blocked user's queue must not occupy every due-batch slot indefinitely.
		operation.deferQueueCheck(now.plus(properties.getLease()));
		mongo.save(operation);
	}
	private void validateTarget(LogoutAllOperation op) {
		var binding = identities.findById(op.getBindingId()).orElseThrow(this::targetInvalid);
		if (!op.getUserId().equals(binding.getUserId()) || !op.getProjectId().equals(binding.getFirebaseProjectId())
				|| !op.getFirebaseUid().equals(binding.getFirebaseUid()) || !op.getBindingCreatedAt().equals(binding.getCreatedAt())) throw targetInvalid();
	}
	private void validateSnapshot(LogoutAllOperation op, FirebaseSessionRevocationPort.Snapshot snapshot) {
		if (snapshot == null || snapshot.disabled() || snapshot.createdAt() == null || snapshot.validAfter() == null
				|| (op.getRemoteCreationTime() != null && !op.getRemoteCreationTime().equals(snapshot.createdAt()))) throw targetInvalid();
	}
	private FirebaseSessionRevocationPort.Target target(LogoutAllOperation op) {
		return new FirebaseSessionRevocationPort.Target(op.getProjectId(), op.getTenantId(), op.getFirebaseUid());
	}
	private FirebaseSessionRevocationPort.Failure targetInvalid() {
		return new FirebaseSessionRevocationPort.Failure(FirebaseSessionRevocationPort.Failure.Kind.TARGET_INVALID);
	}
}
