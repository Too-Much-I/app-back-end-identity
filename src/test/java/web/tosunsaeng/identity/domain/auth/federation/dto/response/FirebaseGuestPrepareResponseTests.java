package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class FirebaseGuestPrepareResponseTests {

	@Test
	void enrollmentResponseCopiesRequirementsAndNormalizesPolicyVersions() {
		EnumSet<FirebaseEnrollmentRequirement> requirements = EnumSet.of(
				FirebaseEnrollmentRequirement.PHONE_VERIFICATION
		);

		FirebaseGuestPrepareResponse response = FirebaseGuestPrepareResponse.enrollmentRequired(
				"550e8400-e29b-41d4-a716-446655440000",
				requirements,
				" privacy-v2 ",
				" term-v2 ",
				600_000
		);
		requirements.add(FirebaseEnrollmentRequirement.PROFILE);

		assertThat(response.missingRequirements()).containsExactly(
				FirebaseEnrollmentRequirement.PHONE_VERIFICATION
		);
		assertThat(response.privacyConsentVersion()).isEqualTo("privacy-v2");
		assertThat(response.termConsentVersion()).isEqualTo("term-v2");
	}

	@Test
	void mergeResponseContainsNoEnrollmentData() {
		FirebaseGuestPrepareResponse response = FirebaseGuestPrepareResponse.mergeRequired();

		assertThat(response.type()).isEqualTo(FirebaseGuestPrepareResultType.MERGE_REQUIRED);
		assertThat(response.enrollmentId()).isNull();
		assertThat(response.missingRequirements()).isNull();
		assertThat(response.privacyConsentVersion()).isNull();
		assertThat(response.termConsentVersion()).isNull();
		assertThat(response.expiresIn()).isNull();
	}

	@Test
	void enrollmentResponseRejectsMissingPolicyVersion() {
		assertThatThrownBy(() -> FirebaseGuestPrepareResponse.enrollmentRequired(
				"550e8400-e29b-41d4-a716-446655440000",
				EnumSet.noneOf(FirebaseEnrollmentRequirement.class),
				null,
				"term-v2",
				600_000
		)).isInstanceOf(NullPointerException.class);
	}
}
