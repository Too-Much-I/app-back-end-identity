package web.tosunsaeng.identity.domain.auth.accountrecovery;

import io.swagger.v3.oas.annotations.media.Schema;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.EmailHint;

public record RecoveryResult(Status status, @Schema(nullable = true) SocialProvider provider,
		@Schema(nullable = true) String maskedEmail, @Schema(nullable = true) EmailHint.Kind emailHintKind) {
	public enum Status { FOUND, NOT_FOUND, ACTION_REQUIRED }
	public static RecoveryResult notFound() { return new RecoveryResult(Status.NOT_FOUND, null, null, null); }
	public static RecoveryResult actionRequired() { return new RecoveryResult(Status.ACTION_REQUIRED, null, null, null); }
	public static RecoveryResult found(SocialIdentity social) {
		return new RecoveryResult(Status.FOUND, social.getProvider(), social.getMaskedEmail(), social.getEmailHintKind());
	}
	@Override public String toString() { return "RecoveryResult[status=" + status + ", hint=REDACTED]"; }
}
