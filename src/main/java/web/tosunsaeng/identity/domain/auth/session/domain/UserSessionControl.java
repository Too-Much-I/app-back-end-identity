package web.tosunsaeng.identity.domain.auth.session.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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
			if (minimumFirebaseAuthTimeExclusive != null) throw loggedOut();
			return;
		}
		if (authentication.epoch() != epoch) throw loggedOut();
		if (authentication.source() != SessionAuthentication.Source.FIREBASE) return;
		if (firebaseBindingId != null && !firebaseBindingId.equals(authentication.firebaseBindingId())) {
			throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		}
		Instant boundary = newAuthentication ? minimumFirebaseAuthTimeExclusive
				: confirmedFirebaseRevocationBoundary;
		if (boundary != null && !authentication.firebaseAuthTime().isAfter(boundary)) {
			throw new AuthException(newAuthentication ? AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED
					: AuthErrorStatus.SESSION_LOGGED_OUT);
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
	private static AuthException loggedOut() { return new AuthException(AuthErrorStatus.SESSION_LOGGED_OUT); }
	public String getUserId() { return userId; }
	public long getSessionEpoch() { return sessionEpoch; }
	public Long getVersion() { return version; }
	public String getFirebaseBindingId() { return firebaseBindingId; }
	public Instant getConfirmedFirebaseRevocationBoundary() { return confirmedFirebaseRevocationBoundary; }
	public Instant getMinimumFirebaseAuthTimeExclusive() { return minimumFirebaseAuthTimeExclusive; }
}
