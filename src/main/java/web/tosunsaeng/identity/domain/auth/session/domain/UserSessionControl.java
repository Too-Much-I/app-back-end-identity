package web.tosunsaeng.identity.domain.auth.session.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import static web.tosunsaeng.identity.domain.auth.common.exception.SessionRejectionReason.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.session.application.SessionAuthentication;

@Document("user_session_controls")
public class UserSessionControl {
	@Id private String userId;
	@Version private Long version;
	private long sessionEpoch;
	private String firebaseBindingId;
	private Instant minimumFirebaseAuthTimeExclusive;
	private Instant confirmedFirebaseRevocationBoundary;
	private long revision;
	private String activeLogoutId;
	private boolean markingRequired;
	// Additive purchase metadata. Null means not migrated, never an ACTIVE default.
	private web.tosunsaeng.identity.domain.user.domain.enums.UserStatus purchaseState;
	private web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType purchaseAccountType;
	private Instant lastPurchaseTokenExpiresAt;

	public void initializePurchaseState(web.tosunsaeng.identity.domain.user.domain.enums.UserStatus state,
			web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType type) {
		Objects.requireNonNull(state); Objects.requireNonNull(type);
		if ((purchaseState != null && purchaseState != state)
				|| (purchaseAccountType != null && purchaseAccountType != type))
			throw new IllegalStateException("Purchase control state conflict.");
		purchaseState = state; purchaseAccountType = type; touch();
	}
	public void changePurchaseState(web.tosunsaeng.identity.domain.user.domain.enums.UserStatus state,
			web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType type) {
		Objects.requireNonNull(state); Objects.requireNonNull(type);
		if (purchaseState != null && purchaseState != web.tosunsaeng.identity.domain.user.domain.enums.UserStatus.ACTIVE
				&& (purchaseState != state || purchaseAccountType != type))
			throw new IllegalStateException("Terminal control cannot be reactivated or retyped.");
		if (state != web.tosunsaeng.identity.domain.user.domain.enums.UserStatus.ACTIVE && purchaseState != state) {
			sessionEpoch = Math.incrementExact(sessionEpoch);
			markingRequired = true;
		}
		purchaseState = state; purchaseAccountType = type; touch();
	}
	public void approvePurchase(Instant expiresAt) {
		if (purchaseState != web.tosunsaeng.identity.domain.user.domain.enums.UserStatus.ACTIVE
				|| purchaseAccountType != web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType.MEMBER)
			throw new IllegalStateException("Purchase requires an active member control.");
		lastPurchaseTokenExpiresAt = max(lastPurchaseTokenExpiresAt, Objects.requireNonNull(expiresAt));
		touch();
	}
	public web.tosunsaeng.identity.domain.user.domain.enums.UserStatus getPurchaseState() { return purchaseState; }
	public web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType getPurchaseAccountType() { return purchaseAccountType; }
	public Instant getLastPurchaseTokenExpiresAt() { return lastPurchaseTokenExpiresAt; }
	public boolean isMarkingRequired() { return markingRequired; }
	public void markingComplete() { markingRequired = false; touch(); }
	public String getActiveLogoutId() { return activeLogoutId; }
	public void claimLogout(String logoutId) {
		if (activeLogoutId != null && !activeLogoutId.equals(logoutId)) throw new IllegalStateException("Logout slot is occupied.");
		activeLogoutId = logoutId; touch();
	}
	public void releaseLogout(String logoutId) {
		if (!Objects.equals(activeLogoutId, logoutId)) throw new IllegalStateException("Logout slot changed.");
		activeLogoutId = null; touch();
	}
	public void requireEpoch(long epoch) { if (epoch != sessionEpoch) throw loggedOut(); }

	private UserSessionControl() { }
	public UserSessionControl(String userId) { this.userId = Objects.requireNonNull(userId); }

	public void touch() { revision = Math.incrementExact(revision); }

	public void logout(String bindingId, Instant requestedAt) {
		sessionEpoch = Math.incrementExact(sessionEpoch);
		markingRequired = true;
		if (bindingId != null) {
			bind(bindingId);
			minimumFirebaseAuthTimeExclusive = max(minimumFirebaseAuthTimeExclusive,
					requestedAt.truncatedTo(ChronoUnit.SECONDS));
		}
		touch();
	}

	public void confirmRevocation(String bindingId, Instant validAfter) {
		bind(bindingId);
		Instant boundary = validAfter.truncatedTo(ChronoUnit.SECONDS);
		if (!boundary.equals(validAfter)) boundary = boundary.plusSeconds(1);
		confirmedFirebaseRevocationBoundary = max(confirmedFirebaseRevocationBoundary, boundary);
		minimumFirebaseAuthTimeExclusive = max(minimumFirebaseAuthTimeExclusive, boundary);
		markingRequired = true;
		touch();
	}

	public void validate(long epoch, SessionAuthentication authentication, boolean newAuthentication) {
		if (epoch != sessionEpoch) throw loggedOut();
		if (authentication == null) {
			if (minimumFirebaseAuthTimeExclusive != null) throw AuthException.loggedOut(LEGACY_AUTH_BOUNDARY);
			return;
		}
		if (authentication.epoch() != epoch) throw AuthException.loggedOut(AUTH_EPOCH_MISMATCH);
		if (authentication.source() != SessionAuthentication.Source.FIREBASE) return;
		if (firebaseBindingId != null && !firebaseBindingId.equals(authentication.firebaseBindingId())) {
			throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		}
		Instant boundary = newAuthentication ? minimumFirebaseAuthTimeExclusive
				: confirmedFirebaseRevocationBoundary;
		if (boundary != null && !authentication.firebaseAuthTime().isAfter(boundary)) {
			if (newAuthentication) throw new AuthException(AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED);
			throw AuthException.loggedOut(FIREBASE_REVOCATION_BOUNDARY);
		}
	}

	private void bind(String bindingId) {
		Objects.requireNonNull(bindingId);
		if (firebaseBindingId != null && !firebaseBindingId.equals(bindingId)) {
			throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		}
		firebaseBindingId = bindingId;
	}
	private static Instant max(Instant a, Instant b) { return a == null || b.isAfter(a) ? b : a; }
	private static AuthException loggedOut() { return AuthException.loggedOut(EPOCH_MISMATCH); }
	public String getUserId() { return userId; }
	public long getSessionEpoch() { return sessionEpoch; }
	public Long getVersion() { return version; }
	public String getFirebaseBindingId() { return firebaseBindingId; }
	public Instant getConfirmedFirebaseRevocationBoundary() { return confirmedFirebaseRevocationBoundary; }
	public Instant getMinimumFirebaseAuthTimeExclusive() { return minimumFirebaseAuthTimeExclusive; }
}
