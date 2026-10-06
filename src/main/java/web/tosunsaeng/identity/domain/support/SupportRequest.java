package web.tosunsaeng.identity.domain.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SupportRequest(@NotNull Category category,
        @NotBlank @Size(min=10, max=2000) String message,
        @Email @Size(max=254) String replyEmail, @Valid Context context) {
    public enum Category { AUTH, GENERAL, REFUND }
    public enum Platform { ANDROID, IOS, WEB, UNKNOWN }
    public record Context(
            @Size(max=64) @Pattern(regexp="[A-Za-z0-9_.-]*") String screen,
            @Size(max=100) @Pattern(regexp="[A-Z0-9_]*") String errorCode,
            @Size(max=128) @Pattern(regexp="[A-Za-z0-9_.:-]*") String requestId,
            @Size(max=32) @Pattern(regexp="[A-Za-z0-9_.+-]*") String appVersion,
            Platform platform) { }
    public SupportRequest {
        message = message == null ? null : message.strip();
        replyEmail = replyEmail == null || replyEmail.isBlank() ? null : replyEmail.strip();
    }
    @Override public String toString() { return "SupportRequest[content=REDACTED]"; }
}
