package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.domain.EmailHint;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;

class SingleSocialIdentityPolicyTests {
	String id = "11111111-1111-4111-8111-111111111111";
	VerifiedFirebasePrincipal proof(FirebaseAuthenticationMethod method, List<VerifiedSocialPrincipal> socials) {
		return new VerifiedFirebasePrincipal("project", "uid", method, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH.plusSeconds(300),
				true, true, "+16505550123", Set.of(method), socials);
	}
	@Test void signupAndUpgradeRequireOneSnsButPasswordOnlyRemainsValid() {
		var google = new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "google");
		SingleSocialIdentityPolicy.enrollment(proof(FirebaseAuthenticationMethod.GOOGLE, List.of(google)));
		SingleSocialIdentityPolicy.enrollment(proof(FirebaseAuthenticationMethod.PASSWORD, List.of()));
		assertThatThrownBy(() -> SingleSocialIdentityPolicy.enrollment(proof(FirebaseAuthenticationMethod.GOOGLE,
				List.of(google, new VerifiedSocialPrincipal(SocialProvider.APPLE, "apple")))))
				.isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.SINGLE_SNS_REQUIRED));
	}
	@Test void onlyExactUserUniqueIndexMapsToSingleSnsPolicy() {
		assertThat(SingleSocialIdentityPolicy.isUserIndexConflict(new org.springframework.dao.DuplicateKeyException(
				"E11000 duplicate key collection: test.social_identities index: uk_social_identities_user_id dup key: redacted"))).isTrue();
		assertThat(SingleSocialIdentityPolicy.isUserIndexConflict(new org.springframework.dao.DuplicateKeyException(
				"index: uk_social_identities_provider_subject dup key: redacted"))).isFalse();
	}
	@Test void onlyApprovedSubjectGrantsMemberAuthorityAndPhoneNeverDoes() {
		var identity = SocialIdentity.create(id, SocialProvider.GOOGLE, "google", Instant.EPOCH);
		SingleSocialIdentityPolicy.owned(proof(FirebaseAuthenticationMethod.GOOGLE,
				List.of(new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "google"), new VerifiedSocialPrincipal(SocialProvider.APPLE, "unapproved"))), id, List.of(identity));
		assertThatThrownBy(() -> SingleSocialIdentityPolicy.owned(proof(FirebaseAuthenticationMethod.APPLE,
				List.of(new VerifiedSocialPrincipal(SocialProvider.APPLE, "unapproved"))), id, List.of(identity))).isInstanceOf(AuthException.class);
		assertThatThrownBy(() -> SingleSocialIdentityPolicy.owned(proof(FirebaseAuthenticationMethod.GOOGLE,
				List.of(new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "replacement"))), id, List.of(identity))).isInstanceOf(AuthException.class);
		assertThatThrownBy(() -> SingleSocialIdentityPolicy.owned(proof(FirebaseAuthenticationMethod.PHONE, List.of()), id, List.of())).isInstanceOf(AuthException.class);
		SingleSocialIdentityPolicy.owned(proof(FirebaseAuthenticationMethod.PASSWORD, List.of()), id, List.of());
	}
	@Test void emailHintsAreMaskedOrUnavailableAndOlderSnapshotsCannotOverwrite() {
		assertThat(EmailHint.from(SocialProvider.GOOGLE, "alice@Example.com").maskedEmail()).isEqualTo("a***@example.com");
		assertThat(EmailHint.from(SocialProvider.GOOGLE, "a@example.com").maskedEmail()).isEqualTo("***@example.com");
		assertThat(EmailHint.from(SocialProvider.APPLE, "private@privaterelay.appleid.com")).isEqualTo(new EmailHint(null, EmailHint.Kind.APPLE_PRIVATE_RELAY));
		for (String invalid : Arrays.asList(null, "", "not-email", "x\n@example.com", "x".repeat(65) + "@example.com", "<script>@example.com")) {
			assertThat(EmailHint.from(SocialProvider.GOOGLE, invalid)).isEqualTo(EmailHint.unavailable());
		}
		var identity = SocialIdentity.create(id, SocialProvider.GOOGLE, "subject", Instant.EPOCH)
				.withEmailHint(EmailHint.from(SocialProvider.GOOGLE, "alice@example.com"), Instant.EPOCH.plusSeconds(2))
				.withEmailHint(EmailHint.from(SocialProvider.GOOGLE, "bob@example.com"), Instant.EPOCH.plusSeconds(1));
		assertThat(identity.getMaskedEmail()).isEqualTo("a***@example.com");
	}
}
