package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.application.firebase.FirebaseAuthenticationMethod;
import web.tosunsaeng.identity.domain.auth.application.firebase.FirebaseVerificationPurpose;
import web.tosunsaeng.identity.domain.auth.application.firebase.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;

class FirebaseAdminAuthenticationVerifierTests {

	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "opaque-firebase-uid";
	private static final String PROVIDER_SUBJECT = "opaque-google-subject";
	private static final String ID_TOKEN = "test-only-firebase-id-token";
	private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

	@Test
	void verifiesEnabledGooglePrincipalWithRevokeCheckAndRedactedResult() {
		StubFirebaseAdminClient client = new StubFirebaseAdminClient(validGoogleData(NOW.minusSeconds(60)));
		FirebaseAdminAuthenticationVerifier verifier = verifier(client, properties(true, true, false));

		VerifiedFirebasePrincipal principal = verifier.verify(
				ID_TOKEN,
				FirebaseVerificationPurpose.LOGIN_EXCHANGE
		);

		assertThat(client.checkRevoked).isTrue();
		assertThat(principal.firebaseProjectId()).isEqualTo(PROJECT_ID);
		assertThat(principal.firebaseUid()).isEqualTo(FIREBASE_UID);
		assertThat(principal.signInMethod()).isEqualTo(FirebaseAuthenticationMethod.GOOGLE);
		assertThat(principal.linkedMethods()).containsExactlyInAnyOrder(
				FirebaseAuthenticationMethod.GOOGLE,
				FirebaseAuthenticationMethod.PHONE
		);
		assertThat(principal.linkedSocialPrincipals())
				.singleElement()
				.satisfies(social -> {
					assertThat(social.provider()).isEqualTo(SocialProvider.GOOGLE);
					assertThat(social.providerSubject()).isEqualTo(PROVIDER_SUBJECT);
				});
		assertThat(principal.toString())
				.doesNotContain(ID_TOKEN)
				.doesNotContain(FIREBASE_UID)
				.doesNotContain(PROVIDER_SUBJECT)
				.contains("firebaseUid=[REDACTED]");
	}

	@Test
	void everyFirebaseVerificationPurposeChecksRevocation() {
		for (FirebaseVerificationPurpose purpose : FirebaseVerificationPurpose.values()) {
			StubFirebaseAdminClient client = new StubFirebaseAdminClient(
					validGoogleData(NOW.minusSeconds(30))
			);

			assertThat(verifier(client, properties(true, true, false)).verify(
					ID_TOKEN,
					purpose
			)).isNotNull();
			assertThat(client.checkRevoked).as(purpose.name()).isTrue();
		}
	}

	@Test
	void mapsEnabledAppleAndKakaoProviderSubjectsWithoutEmailIdentity() {
		for (ProviderCase providerCase : List.of(
				new ProviderCase(
						"apple.com",
						FirebaseAuthenticationMethod.APPLE,
						SocialProvider.APPLE,
						false
				),
				new ProviderCase(
						"oidc.kakao",
						FirebaseAuthenticationMethod.KAKAO,
						SocialProvider.KAKAO,
						true
				)
		)) {
			FirebaseAdminPrincipalData data = data(
					NOW.minusSeconds(30),
					providerCase.providerId(),
					false,
					false,
					false,
					List.of(new FirebaseLinkedProviderData(
							providerCase.providerId(),
							PROVIDER_SUBJECT
					))
			);

			VerifiedFirebasePrincipal principal = verifier(
					new StubFirebaseAdminClient(data),
					properties(false, false, providerCase.kakaoEnabled())
			).verify(ID_TOKEN, FirebaseVerificationPurpose.LOGIN_EXCHANGE);

			assertThat(principal.signInMethod()).isEqualTo(providerCase.method());
			assertThat(principal.linkedSocialPrincipals())
					.singleElement()
					.satisfies(social -> {
						assertThat(social.provider()).isEqualTo(providerCase.socialProvider());
						assertThat(social.providerSubject()).isEqualTo(PROVIDER_SUBJECT);
					});
		}
	}

	@Test
	void loginAllowsExactFifteenMinuteBoundaryButRejectsOlderAuthentication() {
		StubFirebaseAdminClient boundaryClient = new StubFirebaseAdminClient(
				validGoogleData(NOW.minus(Duration.ofMinutes(15)))
		);
		assertThat(verifier(boundaryClient, properties(true, true, false)).verify(
				ID_TOKEN,
				FirebaseVerificationPurpose.LOGIN_EXCHANGE
		)).isNotNull();

		StubFirebaseAdminClient staleClient = new StubFirebaseAdminClient(
				validGoogleData(NOW.minus(Duration.ofMinutes(15)).minusSeconds(1))
		);
		assertAuthError(
				() -> verifier(staleClient, properties(true, true, false)).verify(
						ID_TOKEN,
						FirebaseVerificationPurpose.LOGIN_EXCHANGE
				),
				AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED
		);
	}

	@Test
	void highRiskPurposesUseFiveMinuteRecentAuthenticationLimit() {
		StubFirebaseAdminClient staleClient = new StubFirebaseAdminClient(
				validGoogleData(NOW.minus(Duration.ofMinutes(5)).minusSeconds(1))
		);

		assertAuthError(
				() -> verifier(staleClient, properties(true, true, false)).verify(
						ID_TOKEN,
						FirebaseVerificationPurpose.GUEST_MERGE
				),
				AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED
		);
	}

	@Test
	void loginRejectsPhoneSignInEvenWhenPrimaryProviderIsLinked() {
		FirebaseAdminPrincipalData phoneSignIn = data(
				NOW.minusSeconds(30),
				"phone",
				true,
				true,
				false,
				List.of(
						new FirebaseLinkedProviderData("google.com", PROVIDER_SUBJECT),
						new FirebaseLinkedProviderData("phone", null)
				)
		);

		assertAuthError(
				() -> verifier(
						new StubFirebaseAdminClient(phoneSignIn),
						properties(true, true, false)
				).verify(ID_TOKEN, FirebaseVerificationPurpose.LOGIN_EXCHANGE),
				AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED
		);
	}

	@Test
	void enrollmentRequiresSameAccountPhoneAndVerifiedPasswordEmail() {
		FirebaseAdminPrincipalData withoutPhone = data(
				NOW.minusSeconds(30),
				"password",
				true,
				false,
				false,
				List.of(new FirebaseLinkedProviderData("password", null))
		);
		assertAuthError(
				() -> verifier(
						new StubFirebaseAdminClient(withoutPhone),
						properties(false, true, false)
				).verify(ID_TOKEN, FirebaseVerificationPurpose.DIRECT_ENROLLMENT),
				AuthErrorStatus.FIREBASE_PHONE_VERIFICATION_REQUIRED
		);

		FirebaseAdminPrincipalData unverifiedEmail = data(
				NOW.minusSeconds(30),
				"password",
				false,
				true,
				false,
				List.of(
						new FirebaseLinkedProviderData("password", null),
						new FirebaseLinkedProviderData("phone", null)
				)
		);
		assertAuthError(
				() -> verifier(
						new StubFirebaseAdminClient(unverifiedEmail),
						properties(false, true, false)
				).verify(ID_TOKEN, FirebaseVerificationPurpose.DIRECT_ENROLLMENT),
				AuthErrorStatus.FIREBASE_EMAIL_VERIFICATION_REQUIRED
		);
	}

	@Test
	void disabledProviderAndMismatchedProjectFailClosed() {
		assertAuthError(
				() -> verifier(
						new StubFirebaseAdminClient(validGoogleData(NOW.minusSeconds(30))),
						properties(false, true, false)
				).verify(ID_TOKEN, FirebaseVerificationPurpose.LOGIN_EXCHANGE),
				AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED
		);

		FirebaseAdminPrincipalData wrongAudience = new FirebaseAdminPrincipalData(
				PROJECT_ID,
				FIREBASE_UID,
				null,
				"https://securetoken.google.com/" + PROJECT_ID,
				"different-project",
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				"google.com",
				true,
				false,
				true,
				List.of(new FirebaseLinkedProviderData("google.com", PROVIDER_SUBJECT))
		);
		assertAuthError(
				() -> verifier(
						new StubFirebaseAdminClient(wrongAudience),
						properties(true, true, false)
				).verify(ID_TOKEN, FirebaseVerificationPurpose.LOGIN_EXCHANGE),
				AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN
		);
	}

	@Test
	void rejectsFutureOrInternallyInconsistentFirebaseTokenTimes() {
		FirebaseAdminPrincipalData futureIssuedAt = new FirebaseAdminPrincipalData(
				PROJECT_ID,
				FIREBASE_UID,
				null,
				"https://securetoken.google.com/" + PROJECT_ID,
				PROJECT_ID,
				NOW.minusSeconds(10),
				NOW.plusSeconds(31),
				NOW.plusSeconds(300),
				"google.com",
				true,
				false,
				true,
				List.of(new FirebaseLinkedProviderData("google.com", PROVIDER_SUBJECT))
		);
		assertAuthError(
				() -> verifier(
						new StubFirebaseAdminClient(futureIssuedAt),
						properties(true, true, false)
				).verify(ID_TOKEN, FirebaseVerificationPurpose.LOGIN_EXCHANGE),
				AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN
		);

		FirebaseAdminPrincipalData authAfterIssue = new FirebaseAdminPrincipalData(
				PROJECT_ID,
				FIREBASE_UID,
				null,
				"https://securetoken.google.com/" + PROJECT_ID,
				PROJECT_ID,
				NOW.minusSeconds(5),
				NOW.minusSeconds(10),
				NOW.plusSeconds(300),
				"google.com",
				true,
				false,
				true,
				List.of(new FirebaseLinkedProviderData("google.com", PROVIDER_SUBJECT))
		);
		assertAuthError(
				() -> verifier(
						new StubFirebaseAdminClient(authAfterIssue),
						properties(true, true, false)
				).verify(ID_TOKEN, FirebaseVerificationPurpose.LOGIN_EXCHANGE),
				AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN
		);
	}

	@Test
	void clientFailureReasonsMapToStableErrorsWithoutSdkMessage() {
		for (ErrorMapping mapping : List.of(
				new ErrorMapping(
						FirebaseAdminClientException.Reason.INVALID_TOKEN,
						AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN
				),
				new ErrorMapping(
						FirebaseAdminClientException.Reason.ACCOUNT_NOT_ALLOWED,
						AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED
				),
				new ErrorMapping(
						FirebaseAdminClientException.Reason.RATE_LIMITED,
						AuthErrorStatus.FIREBASE_RATE_LIMITED
				),
				new ErrorMapping(
						FirebaseAdminClientException.Reason.UNAVAILABLE,
						AuthErrorStatus.FIREBASE_UNAVAILABLE
				)
		)) {
			StubFirebaseAdminClient client = new StubFirebaseAdminClient(validGoogleData(NOW));
			client.failure = new FirebaseAdminClientException(mapping.reason());
			assertAuthError(
					() -> verifier(client, properties(true, true, false)).verify(
							ID_TOKEN,
							FirebaseVerificationPurpose.LOGIN_EXCHANGE
					),
					mapping.errorStatus()
			);
		}
	}

	private FirebaseAdminAuthenticationVerifier verifier(
			FirebaseAdminClient client,
			FirebaseAuthProperties properties
	) {
		return new FirebaseAdminAuthenticationVerifier(
				client,
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	private FirebaseAdminPrincipalData validGoogleData(Instant authTime) {
		return data(
				authTime,
				"google.com",
				true,
				true,
				false,
				List.of(
						new FirebaseLinkedProviderData("google.com", PROVIDER_SUBJECT),
						new FirebaseLinkedProviderData("phone", null)
				)
		);
	}

	private FirebaseAdminPrincipalData data(
			Instant authTime,
			String signInProvider,
			boolean emailVerified,
			boolean phoneVerified,
			boolean disabled,
			List<FirebaseLinkedProviderData> providers
	) {
		return new FirebaseAdminPrincipalData(
				PROJECT_ID,
				FIREBASE_UID,
				null,
				"https://securetoken.google.com/" + PROJECT_ID,
				PROJECT_ID,
				authTime,
				NOW.minusSeconds(10),
				NOW.plusSeconds(300),
				signInProvider,
				emailVerified,
				disabled,
				phoneVerified,
				providers
		);
	}

	private FirebaseAuthProperties properties(
			boolean googleEnabled,
			boolean phoneEnabled,
			boolean kakaoEnabled
	) {
		return new FirebaseAuthProperties(
				true,
				PROJECT_ID,
				null,
				googleEnabled,
				true,
				kakaoEnabled,
				phoneEnabled,
				"oidc.kakao",
				Duration.ofMinutes(15),
				Duration.ofMinutes(5),
				Duration.ofSeconds(30),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}

	private void assertAuthError(Runnable action, AuthErrorStatus expected) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(
						AuthException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(expected)
				)
				.hasMessage(expected.getMessage())
				.hasMessageNotContaining(ID_TOKEN);
	}

	private static final class StubFirebaseAdminClient implements FirebaseAdminClient {

		private final FirebaseAdminPrincipalData data;
		private FirebaseAdminClientException failure;
		private boolean checkRevoked;

		private StubFirebaseAdminClient(FirebaseAdminPrincipalData data) {
			this.data = data;
		}

		@Override
		public FirebaseAdminPrincipalData verify(String firebaseIdToken, boolean checkRevoked) {
			this.checkRevoked = checkRevoked;
			if (failure != null) {
				throw failure;
			}
			return data;
		}
	}

	private record ErrorMapping(
			FirebaseAdminClientException.Reason reason,
			AuthErrorStatus errorStatus
	) {
	}

	private record ProviderCase(
			String providerId,
			FirebaseAuthenticationMethod method,
			SocialProvider socialProvider,
			boolean kakaoEnabled
	) {
	}
}
