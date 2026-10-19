package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequirement;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;

public final class FirebaseEnrollmentRequirementResolver {

	public Set<FirebaseEnrollmentRequirement> resolveDirectSignup(
			VerifiedFirebasePrincipal principal
	) {
		EnumSet<FirebaseEnrollmentRequirement> requirements = verificationRequirements(
				principal
		);
		requirements.add(FirebaseEnrollmentRequirement.PROFILE);
		requirements.add(FirebaseEnrollmentRequirement.CONSENTS);
		return Set.copyOf(requirements);
	}

	public Set<FirebaseEnrollmentRequirement> resolveGuestUpgrade(
			VerifiedFirebasePrincipal principal,
			User guest,
			ConsentPolicy consentPolicy
	) {
		User requiredGuest = Objects.requireNonNull(guest, "guest must not be null");
		ConsentPolicy requiredPolicy = Objects.requireNonNull(
				consentPolicy,
				"consentPolicy must not be null"
		);
		EnumSet<FirebaseEnrollmentRequirement> requirements = verificationRequirements(
				principal
		);
		// Guest의 기본 nickname은 완료된 MEMBER profile이 아니다.
		requirements.add(FirebaseEnrollmentRequirement.PROFILE);
		if (!hasCurrentRequiredConsents(requiredGuest.getConsents(), requiredPolicy)) {
			requirements.add(FirebaseEnrollmentRequirement.CONSENTS);
		}
		return Set.copyOf(requirements);
	}

	private EnumSet<FirebaseEnrollmentRequirement> verificationRequirements(
			VerifiedFirebasePrincipal principal
	) {
		VerifiedFirebasePrincipal requiredPrincipal = Objects.requireNonNull(
				principal,
				"principal must not be null"
		);
		EnumSet<FirebaseEnrollmentRequirement> requirements = EnumSet.noneOf(
				FirebaseEnrollmentRequirement.class
		);
		if (requiredPrincipal.linkedMethods().contains(FirebaseAuthenticationMethod.PASSWORD)
				&& !requiredPrincipal.emailVerified()) {
			requirements.add(FirebaseEnrollmentRequirement.EMAIL_VERIFICATION);
		}
		if (!requiredPrincipal.linkedMethods().contains(FirebaseAuthenticationMethod.PHONE)
				|| !requiredPrincipal.phoneVerified()) {
			requirements.add(FirebaseEnrollmentRequirement.PHONE_VERIFICATION);
		}
		return requirements;
	}

	private boolean hasCurrentRequiredConsents(
			UserConsents consents,
			ConsentPolicy consentPolicy
	) {
		UserConsents requiredConsents = Objects.requireNonNull(
				consents,
				"consents must not be null"
		);
		return requiredConsents.isPrivacyConsented()
				&& consentPolicy.getPrivacyConsentVersion().equals(
						requiredConsents.getPrivacyConsentVersion()
				)
				&& requiredConsents.getPrivacyConsentedAt() != null
				&& requiredConsents.isTermConsented()
				&& consentPolicy.getTermConsentVersion().equals(
						requiredConsents.getTermConsentVersion()
				)
				&& requiredConsents.getTermConsentedAt() != null;
	}
}
