package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequirement;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;

class FirebaseEnrollmentRequirementResolverTests {

	private static final Instant NOW = Instant.parse("2026-09-21T00:00:00Z");
	private static final ConsentPolicy POLICY = new ConsentPolicy(
			"privacy-v2",
			"term-v2",
			"quality-v1"
	);
	private final FirebaseEnrollmentRequirementResolver resolver =
			new FirebaseEnrollmentRequirementResolver();

	@Test
	void directSignupRequiresEveryMissingVerificationAndSignupField() {
		VerifiedFirebasePrincipal principal = principal(
				false,
				false,
				Set.of(FirebaseAuthenticationMethod.PASSWORD)
		);

		assertThat(resolver.resolveDirectSignup(principal)).containsExactlyInAnyOrder(
				FirebaseEnrollmentRequirement.EMAIL_VERIFICATION,
				FirebaseEnrollmentRequirement.PHONE_VERIFICATION,
				FirebaseEnrollmentRequirement.PROFILE,
				FirebaseEnrollmentRequirement.CONSENTS
		);
	}

	@Test
	void guestWithVerifiedPhoneAndCurrentRequiredConsentsOnlyNeedsProfile() {
		User guest = guest(UserConsents.consented("privacy-v2", "term-v2", NOW));
		VerifiedFirebasePrincipal principal = principal(
				true,
				true,
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.PHONE)
		);

		assertThat(resolver.resolveGuestUpgrade(principal, guest, POLICY))
				.containsExactly(FirebaseEnrollmentRequirement.PROFILE);
	}

	@Test
	void guestWithStaleRequiredConsentNeedsConsentAgain() {
		User guest = guest(UserConsents.consented("privacy-v1", "term-v2", NOW));
		VerifiedFirebasePrincipal principal = principal(
				true,
				true,
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.PHONE)
		);

		assertThat(resolver.resolveGuestUpgrade(principal, guest, POLICY))
				.containsExactlyInAnyOrder(
						FirebaseEnrollmentRequirement.PROFILE,
						FirebaseEnrollmentRequirement.CONSENTS
				);
	}

	@Test
	void guestRequirementsUseFreshFirebaseEmailAndPhoneProof() {
		User guest = guest(UserConsents.consented("privacy-v2", "term-v2", NOW));
		VerifiedFirebasePrincipal principal = principal(
				false,
				false,
				Set.of(FirebaseAuthenticationMethod.PASSWORD)
		);

		assertThat(resolver.resolveGuestUpgrade(principal, guest, POLICY))
				.containsExactlyInAnyOrder(
						FirebaseEnrollmentRequirement.EMAIL_VERIFICATION,
						FirebaseEnrollmentRequirement.PHONE_VERIFICATION,
						FirebaseEnrollmentRequirement.PROFILE
				);
	}

	private User guest(UserConsents consents) {
		return User.createGuest("A".repeat(43), "게스트", consents, NOW);
	}

	private VerifiedFirebasePrincipal principal(
			boolean emailVerified,
			boolean phoneVerified,
			Set<FirebaseAuthenticationMethod> linkedMethods
	) {
		return new VerifiedFirebasePrincipal(
				"test-project",
				"firebase-uid-sensitive",
				linkedMethods.contains(FirebaseAuthenticationMethod.PASSWORD)
						? FirebaseAuthenticationMethod.PASSWORD
						: FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				emailVerified,
				phoneVerified,
				phoneVerified ? "+821012345678" : null,
				linkedMethods,
				List.of()
		);
	}
}
