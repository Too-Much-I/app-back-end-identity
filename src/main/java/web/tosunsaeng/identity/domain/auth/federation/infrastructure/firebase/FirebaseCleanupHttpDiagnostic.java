package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.IncomingHttpResponse;

/** Extracts bounded, allow-listed diagnostics only. Never returns remote text. */
record FirebaseCleanupHttpDiagnostic(int status, String remoteCode) {

	private static final ObjectMapper JSON = new ObjectMapper()
			.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
			.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
	private static final Set<String> ALLOWED_CODES = Set.of(
			"INVALID_ARGUMENT", "INVALID_LOCAL_ID", "MISSING_LOCAL_ID", "USER_NOT_FOUND",
			"USER_DISABLED", "INVALID_ID_TOKEN", "TOKEN_EXPIRED", "CREDENTIAL_MISMATCH",
			"INVALID_CREDENTIAL", "INSUFFICIENT_PERMISSION", "PERMISSION_DENIED",
			"UNAUTHENTICATED", "ADMIN_ONLY_OPERATION", "OPERATION_NOT_ALLOWED",
			"PROJECT_NOT_FOUND", "CONFIGURATION_NOT_FOUND", "TENANT_NOT_FOUND",
			"TENANT_ID_MISMATCH", "INVALID_TENANT_ID", "UNSUPPORTED_TENANT_OPERATION",
			"INVALID_PHONE_NUMBER", "PHONE_NUMBER_EXISTS", "INVALID_EMAIL", "EMAIL_EXISTS",
			"TOO_MANY_ATTEMPTS_TRY_LATER", "QUOTA_EXCEEDED", "INTERNAL_ERROR");

	static FirebaseCleanupHttpDiagnostic from(IncomingHttpResponse response) {
		if (response == null) return new FirebaseCleanupHttpDiagnostic(0, "NONE");
		int status = response.getStatusCode();
		// Invalid status values are not propagated as arbitrary diagnostic data.
		status = status >= 100 && status <= 599 ? status : 0;
		return new FirebaseCleanupHttpDiagnostic(status, code(response.getContent()));
	}

	private static String code(String content) {
		if (content == null || content.isBlank()) return "NONE";
		if (content.length() > 16_384) return "RESPONSE_TOO_LARGE";
		try {
			JsonNode root = JSON.readTree(content);
			JsonNode message = root.path("error").path("message");
			if (!message.isTextual()) return "UNRECOGNIZED";
			String text = message.textValue();
			int separator = text.indexOf(':');
			String candidate = separator < 0 ? text : text.substring(0, separator);
			// Only exact protocol codes are accepted; colon-delimited details are discarded.
			return ALLOWED_CODES.contains(candidate) ? candidate : "UNRECOGNIZED";
		} catch (Exception ignored) {
			// Parsing diagnostics must not change cleanup behavior or expose parser messages.
			return "UNPARSEABLE";
		}
	}
}
