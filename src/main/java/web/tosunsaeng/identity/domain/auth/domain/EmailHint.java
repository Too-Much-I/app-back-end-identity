package web.tosunsaeng.identity.domain.auth.domain;

import java.util.Locale;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

/** Display-only, never an authentication or ownership identifier. Raw email is not retained. */
public record EmailHint(String maskedEmail, Kind kind) {
	public enum Kind { EMAIL, APPLE_PRIVATE_RELAY, UNAVAILABLE }
	public static EmailHint unavailable() { return new EmailHint(null, Kind.UNAVAILABLE); }
	public static EmailHint from(SocialProvider provider, String email) {
		if (email == null || email.length() > 254 || !email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+")) return unavailable();
		int at = email.lastIndexOf('@');
		if (at > 64) return unavailable();
		String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
		if (provider == SocialProvider.APPLE && domain.equals("privaterelay.appleid.com")) {
			return new EmailHint(null, Kind.APPLE_PRIVATE_RELAY);
		}
		return new EmailHint((at > 1 ? email.substring(0, 1) : "") + "***@" + domain, Kind.EMAIL);
	}
	@Override public String toString() { return "EmailHint[kind=" + kind + ", maskedEmail=REDACTED]"; }
}
