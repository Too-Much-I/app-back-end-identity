package web.tosunsaeng.identity.domain.auth.providerchange;

import web.tosunsaeng.identity.global.observability.FailureDiagnostic;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort.Failure;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import static web.tosunsaeng.identity.domain.auth.providerchange.ProviderUnlinkOperation.State.*;

public class ProviderUnlinkWorker {
	private ProviderChangeMetrics metrics;
	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setMetrics(ProviderChangeMetrics metrics) { this.metrics = metrics; }
	private void record(ProviderChangeMetrics.Outcome outcome) { if (metrics != null) metrics.record(outcome); }
	private final MongoTemplate mongo;
	private final SessionSecurityService security;
	private final FirebaseIdentityRepository identities;
	private final SocialIdentityRepository socials;
	private final FirebaseProviderMutationPort port;
	private final ProviderChangeProperties properties;
	private final String tenant;
	private final Clock clock;
	public ProviderUnlinkWorker(MongoTemplate mongo, SessionSecurityService security, FirebaseIdentityRepository identities,
			SocialIdentityRepository socials, FirebaseProviderMutationPort port, ProviderChangeProperties properties, String tenant, Clock clock) {
		this.mongo = mongo; this.security = security; this.identities = identities; this.socials = socials;
		this.port = port; this.properties = properties; this.tenant = tenant; this.clock = clock;
	}
	public void runBatch() {
		security.markInvalidSessionsBatch(clock.instant());
		var ops = mongo.find(Query.query(Criteria.where("state").nin(COMPLETED, SUPERSEDED, RECONCILIATION_REQUIRED)
				.and("nextAttemptAt").lte(clock.instant()).orOperator(Criteria.where("leaseUntil").is(null),
						Criteria.where("leaseUntil").lte(clock.instant())))
				.with(Sort.by("acceptedAt")).limit(properties.getBatchSize()), ProviderUnlinkOperation.class);
		for (var op : ops) {
			if (metrics != null) metrics.pendingAge(java.time.Duration.between(op.getAcceptedAt(), clock.instant()));
			try { process(op.getOperationId()); }
			catch (RuntimeException exception) {
				warnFailure(exception);
			}
		}
	}
	public void process(String id) {
		String owner = UUID.randomUUID().toString();
		var op = security.transaction(() -> {
			var current = mongo.findById(id, ProviderUnlinkOperation.class);
			Instant now = clock.instant();
			if (current == null || current.terminal() || current.getState() == RECONCILIATION_REQUIRED
					|| current.getNextAttemptAt().isAfter(now)
					|| (current.getLeaseUntil() != null && current.getLeaseUntil().isAfter(now))) return null;
			var control = security.control(current.getUserId());
			if (!current.slot().equals(control.getActiveLogoutId())) { current.unknown("SLOT_CHANGED"); mongo.save(current); return null; }
			if (current.inFlight()) { current.unknown("UNRESOLVED_DISPATCH"); mongo.save(current); record(ProviderChangeMetrics.Outcome.RECONCILIATION); return null; }
			control.touch(); mongo.save(control);
			current.claim(owner, now.plus(properties.getLease())); mongo.save(current); return current;
		});
		if (op == null) return;
		try {
			var binding = target(op);
			var snapshot = port.inspect(remote(binding)); validate(op, snapshot);
			if (op.getState() == PENDING) {
				SocialIdentity social = exactSocial(op);
				if (!Objects.equals(social.getProviderSubject(), snapshot.providers().get(op.getProvider()))) throw invalid();
				op = start(op, owner, UNLINK_STARTED, snapshot.createdAt());
				if (op == null) return;
				try { port.unlink(remote(binding), op.getProvider()); }
				catch (RuntimeException exception) { unknown(id, owner, "UNLINK_RESULT_UNKNOWN"); return; }
				op = ack(id, owner);
				if (op == null) return;
				snapshot = port.inspect(remote(binding)); validate(op, snapshot);
			}
			if (snapshot.providers().containsKey(op.getProvider())) throw invalid();
			if (op.getState() == UNLINK_ACKED) {
				op = start(op, owner, REVOKE_STARTED, snapshot.createdAt());
				if (op == null) return;
				try { port.revoke(remote(binding), op.getDispatchAt()); }
				catch (RuntimeException exception) {
					observeUnknownRevocation(id, owner, binding);
					unknown(id, owner, "REVOKE_RESULT_UNKNOWN"); return;
				}
				op = ack(id, owner);
				if (op == null) return;
				snapshot = port.inspect(remote(binding)); validate(op, snapshot);
			}
			if (op.getState() != REVOKE_ACKED || snapshot.providers().containsKey(op.getProvider())) throw invalid();
			if (snapshot.validAfter().isBefore(op.getDispatchAt().truncatedTo(ChronoUnit.SECONDS))) {
				throw new Failure(Failure.Kind.READ_TRANSIENT);
			}
			final var evidence = snapshot;
			security.transaction(() -> {
				var current = owned(id, owner);
				if (current == null || current.getState() != REVOKE_ACKED) return null;
				target(current); validate(current, evidence); exactSocial(current);
				var control = security.control(current.getUserId());
				if (!current.slot().equals(control.getActiveLogoutId())) throw invalid();
				control.confirmRevocation(current.getBindingId(), evidence.validAfter());
				var result = mongo.remove(Query.query(Criteria.where("_id").is(current.getTargetSocialIdentityId())
						.and("userId").is(current.getUserId()).and("provider").is(current.getProvider())), SocialIdentity.class);
				if (result.getDeletedCount() != 1) throw invalid();
				current.finish(COMPLETED, clock.instant(), clock.instant().plus(properties.getRetention()));
				control.releaseLogout(current.slot()); mongo.save(control); mongo.save(current);
				record(ProviderChangeMetrics.Outcome.COMPLETED); return null;
			});
		} catch (Failure failure) {
			if (failure.kind() == Failure.Kind.READ_TRANSIENT) retry(id, owner);
			else unknown(id, owner, failure.kind().name());
		} catch (RuntimeException exception) {
			// Includes a lost local commit acknowledgement. No remote replay is inferred from this exception.
			warnFailure(exception);
			unknown(id, owner, "LOCAL_RESULT_UNRESOLVED");
		}
	}
	private void warnFailure(RuntimeException exception) {
		FailureDiagnostic.from(exception, FailureDiagnostic.Operation.PROVIDER_CHANGE_BATCH)
				.attachTo(org.slf4j.LoggerFactory.getLogger(getClass()).atWarn())
				.log("Provider change batch requires retry or reconciliation.");
	}
	private ProviderUnlinkOperation start(ProviderUnlinkOperation op, String owner, ProviderUnlinkOperation.State phase, Instant created) {
		return security.transaction(() -> {
			var current = owned(op.getOperationId(), owner);
			if (current == null || !current.getLeaseUntil().isAfter(clock.instant())) return null;
			security.requireActive(current.getUserId()); target(current);
			var control = security.control(current.getUserId());
			if (!current.slot().equals(control.getActiveLogoutId())) throw invalid();
			control.touch(); mongo.save(control);
			current.start(phase, created, clock.instant()); mongo.save(current); return current;
		});
	}
	private ProviderUnlinkOperation ack(String id, String owner) {
		return security.transaction(() -> {
			var current = owned(id, owner);
			if (current == null) return null;
			if (metrics != null) metrics.phaseLatency(current.getState(), java.time.Duration.between(current.getDispatchAt(), clock.instant()));
			current.acknowledge(); mongo.save(current); return current;
		});
	}
	private FirebaseIdentity target(ProviderUnlinkOperation op) {
		var binding = identities.findById(op.getBindingId()).orElseThrow(this::invalid);
		if (!op.getUserId().equals(binding.getUserId()) || !op.getBindingCreatedAt().equals(binding.getCreatedAt())
				|| !op.getFirebaseProjectId().equals(binding.getFirebaseProjectId())
				|| !op.getFirebaseUid().equals(binding.getFirebaseUid()) || !Objects.equals(op.getFirebaseTenantId(), tenant)) throw invalid();
		return binding;
	}
	private SocialIdentity exactSocial(ProviderUnlinkOperation op) {
		var social = socials.findById(op.getTargetSocialIdentityId()).orElseThrow(this::invalid);
		if (!op.getUserId().equals(social.getUserId()) || op.getProvider() != social.getProvider()) throw invalid();
		return social;
	}
	private void validate(ProviderUnlinkOperation op, FirebaseProviderMutationPort.Snapshot snapshot) {
		if (snapshot == null || snapshot.disabled() || snapshot.createdAt() == null || snapshot.validAfter() == null
				|| (op.getRemoteCreatedAt() != null && !op.getRemoteCreatedAt().equals(snapshot.createdAt()))) throw invalid();
		boolean remaining = op.getRemainingProviders().stream().anyMatch(provider -> {
			String subject = snapshot.providers().get(provider);
			return subject != null && socials.findByProviderAndProviderSubject(provider, subject)
					.filter(s -> s.getUserId().equals(op.getUserId())).isPresent();
		});
		if (!remaining) throw invalid();
	}
	private FirebaseSessionRevocationPort.Target remote(FirebaseIdentity binding) {
		return new FirebaseSessionRevocationPort.Target(binding.getFirebaseProjectId(), tenant, binding.getFirebaseUid());
	}
	private ProviderUnlinkOperation owned(String id, String owner) {
		var op = mongo.findById(id, ProviderUnlinkOperation.class);
		return op != null && !op.terminal() && op.getState() != RECONCILIATION_REQUIRED
				&& owner.equals(op.getLeaseOwner()) ? op : null;
	}
	private void unknown(String id, String owner, String code) {
		security.transaction(() -> {
			var op = owned(id, owner);
			if (op != null) { op.unknown(code); mongo.save(op); record(ProviderChangeMetrics.Outcome.RECONCILIATION); }
			return null;
		});
	}
	private void observeUnknownRevocation(String id, String owner, FirebaseIdentity binding) {
		try {
			var evidence = port.inspect(remote(binding));
			security.transaction(() -> {
				var op = owned(id, owner);
				if (op == null || op.getState() != REVOKE_STARTED) return null;
				target(op); validate(op, evidence);
				if (evidence.validAfter().isBefore(op.getDispatchAt().truncatedTo(ChronoUnit.SECONDS))) return null;
				var control = security.control(op.getUserId());
				if (!op.slot().equals(control.getActiveLogoutId())) return null;
				control.confirmRevocation(op.getBindingId(), evidence.validAfter()); mongo.save(control); return null;
			});
		} catch (RuntimeException ignored) { /* Still unresolved: retain slot and prohibit redispatch. */ }
	}
	private void retry(String id, String owner) {
		security.transaction(() -> {
			var op = owned(id, owner);
			if (op == null) return null;
			if (op.inFlight() || op.getAttemptCount() + 1 >= properties.getMaxAttempts()) {
				op.unknown("READ_RETRY_EXHAUSTED"); record(ProviderChangeMetrics.Outcome.RECONCILIATION);
			}
			else {
				long cap = Math.min(properties.getMaxBackoff().toMillis(), properties.getInitialBackoff().toMillis()
						* (1L << Math.min(op.getAttemptCount(), 10)));
				op.retry(clock.instant().plusMillis(ThreadLocalRandom.current().nextLong(1, cap + 1)));
				record(ProviderChangeMetrics.Outcome.SAFE_RETRY);
			}
			mongo.save(op); return null;
		});
	}
	private Failure invalid() { return new Failure(Failure.Kind.TARGET_INVALID); }
}
