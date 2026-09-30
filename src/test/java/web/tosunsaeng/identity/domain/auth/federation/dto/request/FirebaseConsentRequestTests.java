package web.tosunsaeng.identity.domain.auth.federation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FirebaseConsentRequestTests {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void missingAndNullFlagsRemainBackwardCompatibleForBothPaths() throws Exception {
		ObjectNode body = body();
		for (boolean explicitNull : new boolean[] {false, true}) {
			if (explicitNull) body.putNull("isQualityReviewConsented");
			FirebaseSignupRequest signup = mapper.treeToValue(body, FirebaseSignupRequest.class);
			FirebaseGuestUpgradeRequest upgrade = mapper.treeToValue(body, FirebaseGuestUpgradeRequest.class);
			assertThat(signup.isQualityReviewConsented()).isFalse();
			assertThat(upgrade.isQualityReviewConsented()).isNull();
			assertValid(signup);
			assertValid(upgrade);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void suppliedFlagsArePreservedAndVersionIsTrimmed(boolean consented) throws Exception {
		ObjectNode body = body().put("isQualityReviewConsented", consented)
				.put("qualityReviewConsentVersion", " quality-review-v1 ");
		FirebaseSignupRequest signup = mapper.treeToValue(body, FirebaseSignupRequest.class);
		FirebaseGuestUpgradeRequest upgrade = mapper.treeToValue(body, FirebaseGuestUpgradeRequest.class);
		assertThat(signup.isQualityReviewConsented()).isEqualTo(consented);
		assertThat(upgrade.isQualityReviewConsented()).isEqualTo(consented);
		assertThat(signup.qualityReviewConsentVersion()).isEqualTo("quality-review-v1");
		assertThat(upgrade.qualityReviewConsentVersion()).isEqualTo("quality-review-v1");
		assertValid(signup);
		assertValid(upgrade);
	}

	@ParameterizedTest
	@ValueSource(strings = {"bad version", "", "bad/version"})
	void invalidVersionSyntaxIsRejectedEvenWhenConsentIsFalse(String version) throws Exception {
		assertInvalidVersion(version);
	}

	@Test
	void oversizedVersionIsRejected() throws Exception {
		assertInvalidVersion("v".repeat(101));
	}

	private void assertInvalidVersion(String version) throws Exception {
		ObjectNode body = body().put("isQualityReviewConsented", false)
				.put("qualityReviewConsentVersion", version);
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			var validator = factory.getValidator();
			for (Class<?> type : new Class<?>[] {FirebaseSignupRequest.class, FirebaseGuestUpgradeRequest.class}) {
				assertThat(validator.validate(mapper.treeToValue(body, type)))
						.isNotEmpty()
						.allSatisfy(violation -> assertThat(violation.getPropertyPath().toString())
								.isEqualTo("qualityReviewConsentVersion"));
			}
		}
	}

	private void assertValid(Object request) {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			assertThat(factory.getValidator().validate(request)).isEmpty();
		}
	}

	private ObjectNode body() {
		return mapper.createObjectNode()
				.put("enrollmentId", "550e8400-e29b-41d4-a716-446655440000")
				.put("firebaseIdToken", "test-only-proof")
				.put("nickname", "테스트회원")
				.put("isPrivacyConsented", true)
				.put("privacyConsentVersion", "privacy-v1")
				.put("isTermConsented", true)
				.put("termConsentVersion", "term-v1");
	}
}
