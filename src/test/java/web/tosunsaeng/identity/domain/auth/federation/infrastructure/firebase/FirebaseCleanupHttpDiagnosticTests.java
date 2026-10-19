package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.IncomingHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FirebaseCleanupHttpDiagnosticTests {

	@ParameterizedTest
	@ValueSource(strings = {"INSUFFICIENT_PERMISSION", "INVALID_LOCAL_ID", "INVALID_PHONE_NUMBER",
			"ADMIN_ONLY_OPERATION", "OPERATION_NOT_ALLOWED", "INVALID_ARGUMENT"})
	void acceptsOnlyKnownCodeAndDiscardsDetail(String code) {
		assertThat(diagnostic(400, "{\"error\":{\"message\":\"" + code
				+ ": fake-token private@example.invalid\"}}"))
				.isEqualTo(new FirebaseCleanupHttpDiagnostic(400, code));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"error\":{\"message\":\"UNKNOWN_SECRET_CODE\"}}",
			"{\"error\":{\"message\":\"INVALID_ARGUMENT fake-token\"}}",
			"{\"error\":{\"message\":\"INVALID_ARGUMENT\\nfake-token\"}}",
			"{\"error\":{\"message\":\" INVALID_ARGUMENT\"}}",
			"{\"error\":{\"message\":123}}", "{\"error\":{\"message\":null}}",
			"{\"error\":{\"message\":{\"secret\":\"fake-token\"}}}", "{}", "null"
	})
	void unknownOrInvalidShapeNeverReturnsRemoteText(String content) {
		assertThat(diagnostic(400, content).remoteCode()).isEqualTo("UNRECOGNIZED");
	}

	@ParameterizedTest
	@ValueSource(strings = {"<html>fake-token</html>", "{",
			"{\"error\":{\"message\":\"INVALID_ARGUMENT\",\"message\":\"fake-token\"}}",
			"{\"error\":{\"message\":\"INVALID_ARGUMENT\"}} {}"})
	void malformedAmbiguousOrTrailingContentIsRejected(String content) {
		assertThat(diagnostic(400, content).remoteCode()).isEqualTo("UNPARSEABLE");
	}

	@Test
	void oversizedAndDeeplyNestedContentCannotDisruptCleanup() {
		assertThat(diagnostic(400, "x".repeat(16_385)).remoteCode()).isEqualTo("RESPONSE_TOO_LARGE");
		assertThat(diagnostic(400, "[".repeat(1100) + "0" + "]".repeat(1100)).remoteCode())
				.isEqualTo("UNPARSEABLE");
	}

	@Test
	void missingResponseAndInvalidStatusUseSafeSentinels() {
		assertThat(FirebaseCleanupHttpDiagnostic.from(null))
				.isEqualTo(new FirebaseCleanupHttpDiagnostic(0, "NONE"));
		assertThat(diagnostic(700, null)).isEqualTo(new FirebaseCleanupHttpDiagnostic(0, "NONE"));
		assertThat(diagnostic(400, " ")).isEqualTo(new FirebaseCleanupHttpDiagnostic(400, "NONE"));
	}

	private FirebaseCleanupHttpDiagnostic diagnostic(int status, String content) {
		IncomingHttpResponse response = mock(IncomingHttpResponse.class);
		when(response.getStatusCode()).thenReturn(status);
		when(response.getContent()).thenReturn(content);
		return FirebaseCleanupHttpDiagnostic.from(response);
	}
}
