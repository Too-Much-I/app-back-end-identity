package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Instant;
import java.util.Objects;

/** Server-verified authentication evidence; never derived from request account-type claims. */
public record SessionAuthentication(long epoch, Source source, String firebaseBindingId,
		Instant firebaseAuthTime) {
	public enum Source { LOCAL, GUEST, FIREBASE }

	public SessionAuthentication {
		if (epoch < 0) throw new IllegalArgumentException("Invalid session epoch.");
		Objects.requireNonNull(source);
		if (source == Source.FIREBASE) {
			if (firebaseBindingId == null || firebaseBindingId.isBlank()) {
				throw new IllegalArgumentException("Firebase binding is required.");
			}
			Objects.requireNonNull(firebaseAuthTime);
		} else if (firebaseBindingId != null || firebaseAuthTime != null) {
			throw new IllegalArgumentException("Unexpected Firebase evidence.");
		}
	}
}
