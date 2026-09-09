package web.tosunsaeng.identity.domain.auth.session.application;

import web.tosunsaeng.identity.domain.auth.session.domain.LogoutAllOperation;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.TransactionException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;

/** All issuance and account mutation writers must contend on this Mongo document. */
public class SessionSecurityService {
	private final MongoTemplate mongo;
	private final TransactionTemplate transactions;
	private final UserRepository users;
	private final FirebaseIdentityRepository identities;
	private Duration retention = Duration.ofDays(7);
	private Duration verifierSkew = Duration.ofMinutes(1);
	public void configureRetention(Duration retention, Duration verifierSkew) {
		this.retention = Objects.requireNonNull(retention); this.verifierSkew = Objects.requireNonNull(verifierSkew);
	}

	public SessionSecurityService(MongoTemplate mongo, TransactionTemplate transactions,
			UserRepository users, FirebaseIdentityRepository identities) {
		this.mongo = Objects.requireNonNull(mongo);
		this.transactions = Objects.requireNonNull(transactions);
		this.users = Objects.requireNonNull(users);
		this.identities = Objects.requireNonNull(identities);
	}

	public <T> T transaction(Supplier<T> work) {
		try { return transactions.execute(status -> work.get()); }
		catch (DataAccessException | TransactionException exception) { throw unavailable(); }
	}

	public <T> T transactionKeepingUniqueConflicts(Supplier<T> work) {
		try { return transactions.execute(status -> work.get()); }
		catch (org.springframework.dao.DuplicateKeyException exception) { throw exception; }
		catch (DataAccessException | TransactionException exception) { throw unavailable(); }
	}

	public long captureEpoch(String userId) {
		try { return control(userId).getSessionEpoch(); }
		catch (DataAccessException exception) { throw unavailable(); }
	}

	public UserSessionControl control(String userId) {
		UserSessionControl control = mongo.findById(userId, UserSessionControl.class);
		return control == null ? new UserSessionControl(userId) : control;
	}

	public void requireActive(String userId) {
		var user = users.findById(userId).orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() == UserStatus.WITHDRAWN) throw new AuthException(AuthErrorStatus.ACCOUNT_WITHDRAWN);
		if (user.getStatus() != UserStatus.ACTIVE) throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
	}
	public boolean requiresFirebaseBinding(String userId) {
		return users.findById(userId).map(user -> user.getProvider()
				== UserProvider.FEDERATED).orElse(false);
	}

	public void checkAndTouch(RefreshSession session, boolean newAuthentication) {
		requireActive(session.getUserId());
		UserSessionControl control = control(session.getUserId());
		var proof = session.getAuthentication();
		if (newAuthentication && proof == null) throw unavailable();
		control.validate(session.getSessionEpoch(), proof, newAuthentication);
		if (proof != null && proof.source() == SessionAuthentication.Source.FIREBASE) {
			var binding = identities.findById(proof.firebaseBindingId())
					.orElseThrow(() -> new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
			if (!binding.getUserId().equals(session.getUserId())) {
				throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
			}
		}
		control.touch();
		mongo.save(control);
	}

	public void checkFirebaseAuthentication(String userId, SessionAuthentication proof) {
		requireActive(userId);
		var control = control(userId);
		control.validate(proof.epoch(), proof, true);
		var binding = identities.findById(proof.firebaseBindingId())
				.orElseThrow(() -> new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
		if (!userId.equals(binding.getUserId())) throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		control.touch(); mongo.save(control);
	}

	public void validateExistingFirebaseProof(VerifiedFirebasePrincipal principal) {
		identities.findByFirebaseProjectIdAndFirebaseUid(principal.firebaseProjectId(), principal.firebaseUid()).ifPresent(binding -> {
			var control = control(binding.getUserId());
			control.validate(control.getSessionEpoch(), new SessionAuthentication(control.getSessionEpoch(),
					SessionAuthentication.Source.FIREBASE, binding.getFirebaseIdentityId(), principal.authTime()), true);
		});
	}

	public boolean hasUnresolvedLogout(String userId) {
		return mongo.exists(Query.query(
				Criteria.where("userId").is(userId)
						.and("status").nin(LogoutAllOperation.Status.COMPLETED,
								LogoutAllOperation.Status.SUPERSEDED_BY_WITHDRAWAL)),
				LogoutAllOperation.class);
	}

	/** Called in the withdrawal transaction. Never abandons an actor which may still dispatch. */
	public void handoffWithdrawal(String userId, Instant now) {
		var control = control(userId);
		var operations = mongo.find(Query.query(
				Criteria.where("userId").is(userId)
						.and("status").nin(LogoutAllOperation.Status.COMPLETED,
								LogoutAllOperation.Status.SUPERSEDED_BY_WITHDRAWAL)),
				LogoutAllOperation.class);
		for (var operation : operations) {
			if (!operation.isMutationStarted()) {
				// Request credentials remain expired well before this minimum retention.
				Instant cleanup = now.plus(retention);
				Instant request = operation.getRequestExpiresAt().plus(verifierSkew);
				operation.supersede(now, cleanup.isAfter(request) ? cleanup : request);
				mongo.save(operation);
				if (operation.getLogoutId().equals(control.getActiveLogoutId())) control.releaseLogout(operation.getLogoutId());
			}
		}
		control.touch(); mongo.save(control);
	}

	public void guardIdentityRelease(String userId) {
		var control = control(userId);
		control.touch(); mongo.save(control);
		if (hasUnresolvedLogout(userId)) throw new AuthException(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING);
	}

	/** Physical marking is bounded and secondary to the logical epoch/authentication fence. */
	public void markInvalidSessionsBatch(Instant now) {
		var controls = mongo.find(Query.query(
				Criteria.where("markingRequired").is(true)).limit(20), UserSessionControl.class);
		for (var selected : controls) transaction(() -> {
			var control = control(selected.getUserId());
			var conditions = new ArrayList<Criteria>();
			conditions.add(Criteria.where("sessionEpoch").lt(control.getSessionEpoch()));
			if (control.getSessionEpoch() > 0) conditions.add(Criteria.where("sessionEpoch").exists(false));
			if (control.getConfirmedFirebaseRevocationBoundary() != null) {
				conditions.add(Criteria.where("authentication.source").is(SessionAuthentication.Source.FIREBASE)
						.and("authentication.firebaseBindingId").is(control.getFirebaseBindingId())
						.and("authentication.firebaseAuthTime").lte(control.getConfirmedFirebaseRevocationBoundary()));
			}
			var predicate = Criteria.where("userId").is(control.getUserId())
					.and("revokedAt").is(null).and("revocationReason").is(null)
					.orOperator(conditions.toArray(Criteria[]::new));
			var sessions = mongo.find(Query.query(predicate).limit(100), RefreshSession.class);
			for (var session : sessions) { session.logoutAll(now); mongo.save(session); }
			if (sessions.size() < 100) control.markingComplete(); else control.touch();
			mongo.save(control); return null;
		});
	}

	public void touchExpected(String userId, long epoch) {
		UserSessionControl control = control(userId);
		control.requireEpoch(epoch);
		control.touch();
		mongo.save(control);
	}

	public SessionAuthentication firebase(String userId, long epoch,
			VerifiedFirebasePrincipal principal) {
		var binding = identities.findByFirebaseProjectIdAndFirebaseUid(
				principal.firebaseProjectId(), principal.firebaseUid())
				.orElseThrow(() -> new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
		if (!binding.getUserId().equals(userId)) throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		return new SessionAuthentication(epoch, SessionAuthentication.Source.FIREBASE,
				binding.getFirebaseIdentityId(), principal.authTime());
	}

	public static AuthException unavailable() { return new AuthException(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE); }
}
