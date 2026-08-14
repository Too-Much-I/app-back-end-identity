package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AbstractFirebaseAuth;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.Test;

class FirebaseSdkAdminClientTests {

	private static final String ID_TOKEN = "test-only-id-token";
	private static final String FIREBASE_UID = "opaque-firebase-uid";
	private static final String GOOGLE_SUBJECT = "opaque-google-subject";

	@Test
	void mapsSdkObjectsToMinimalSnapshotAndDropsPhoneProviderUid() throws Exception {
		AbstractFirebaseAuth firebaseAuth = mock(AbstractFirebaseAuth.class);
		FirebaseToken token = mock(FirebaseToken.class);
		UserRecord user = mock(UserRecord.class);
		UserInfo google = provider("google.com", GOOGLE_SUBJECT);
		UserInfo phone = provider("phone", "+820000000000");
		Instant authTime = Instant.parse("2026-08-13T11:59:00Z");
		Instant issuedAt = Instant.parse("2026-08-13T11:59:10Z");
		Instant expiresAt = Instant.parse("2026-08-13T12:59:10Z");

		when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenReturn(token);
		when(firebaseAuth.getUser(FIREBASE_UID)).thenReturn(user);
		when(token.getUid()).thenReturn(FIREBASE_UID);
		when(token.getTenantId()).thenReturn(null);
		when(token.getIssuer()).thenReturn("https://securetoken.google.com/test-project");
		when(token.getClaims()).thenReturn(Map.of(
				"aud", "test-project",
				"auth_time", authTime.getEpochSecond(),
				"iat", issuedAt.getEpochSecond(),
				"exp", expiresAt.getEpochSecond(),
				"firebase", Map.of("sign_in_provider", "google.com")
		));
		when(user.getProviderData()).thenReturn(new UserInfo[]{google, phone});
		when(user.getPhoneNumber()).thenReturn("+820000000000");
		when(user.isEmailVerified()).thenReturn(true);
		when(user.isDisabled()).thenReturn(false);

		FirebaseAdminPrincipalData data = new FirebaseSdkAdminClient(
				firebaseAuth,
				"test-project"
		).verify(ID_TOKEN, true);

		assertThat(data.firebaseUid()).isEqualTo(FIREBASE_UID);
		assertThat(data.authTime()).isEqualTo(authTime);
		assertThat(data.issuedAt()).isEqualTo(issuedAt);
		assertThat(data.expiresAt()).isEqualTo(expiresAt);
		assertThat(data.emailVerified()).isTrue();
		assertThat(data.phoneVerified()).isTrue();
		assertThat(data.verifiedPhoneNumber()).isEqualTo("+820000000000");
		assertThat(data.linkedProviders())
				.extracting(FirebaseLinkedProviderData::providerId)
				.containsExactly("google.com", "phone");
		assertThat(data.linkedProviders().get(0).providerUid()).isEqualTo(GOOGLE_SUBJECT);
		assertThat(data.linkedProviders().get(1).providerUid()).isNull();
		assertThat(data.toString())
				.doesNotContain(ID_TOKEN)
				.doesNotContain(FIREBASE_UID)
				.doesNotContain(GOOGLE_SUBJECT)
				.doesNotContain("+820000000000");
		verify(firebaseAuth).verifyIdToken(ID_TOKEN, true);
		verify(firebaseAuth).getUser(FIREBASE_UID);
	}

	@Test
	void classifiesQuotaDisabledAndUnavailableWithoutSdkMessage() throws Exception {
		assertSdkFailureMaps(
				new FirebaseAuthException(
						ErrorCode.RESOURCE_EXHAUSTED,
						"internal quota detail",
						null,
						null,
						null
				),
				FirebaseAdminClientException.Reason.RATE_LIMITED
		);
		assertSdkFailureMaps(
				new FirebaseAuthException(
						ErrorCode.PERMISSION_DENIED,
						"internal disabled detail",
						null,
						null,
						AuthErrorCode.USER_DISABLED
				),
				FirebaseAdminClientException.Reason.ACCOUNT_NOT_ALLOWED
		);
		assertSdkFailureMaps(
				new FirebaseAuthException(
						ErrorCode.UNAVAILABLE,
						"internal endpoint detail",
						null,
						null,
						null
				),
				FirebaseAdminClientException.Reason.UNAVAILABLE
		);
		assertSdkFailureMaps(
				new FirebaseAuthException(
						ErrorCode.PERMISSION_DENIED,
						"internal service account permission detail",
						null,
						null,
						null
				),
				FirebaseAdminClientException.Reason.UNAVAILABLE
		);
	}

	@Test
	void preservesMalformedClaimClassificationButTreatsUnexpectedRuntimeAsUnavailable()
			throws Exception {
		AbstractFirebaseAuth malformedClaimAuth = mock(AbstractFirebaseAuth.class);
		FirebaseToken malformedToken = mock(FirebaseToken.class);
		when(malformedClaimAuth.verifyIdToken(ID_TOKEN, true)).thenReturn(malformedToken);
		when(malformedClaimAuth.getUser(FIREBASE_UID)).thenReturn(mock(UserRecord.class));
		when(malformedToken.getUid()).thenReturn(FIREBASE_UID);
		when(malformedToken.getClaims()).thenReturn(Map.of());

		assertClientFailure(
				malformedClaimAuth,
				FirebaseAdminClientException.Reason.INVALID_TOKEN
		);

		AbstractFirebaseAuth unexpectedFailureAuth = mock(AbstractFirebaseAuth.class);
		when(unexpectedFailureAuth.verifyIdToken(ID_TOKEN, true))
				.thenThrow(new IllegalStateException("unexpected internal detail"));

		assertClientFailure(
				unexpectedFailureAuth,
				FirebaseAdminClientException.Reason.UNAVAILABLE
		);
	}

	private void assertSdkFailureMaps(
			FirebaseAuthException sdkException,
			FirebaseAdminClientException.Reason expectedReason
	) throws Exception {
		AbstractFirebaseAuth firebaseAuth = mock(AbstractFirebaseAuth.class);
		when(firebaseAuth.verifyIdToken(ID_TOKEN, true)).thenThrow(sdkException);

		assertThatThrownBy(() -> new FirebaseSdkAdminClient(
				firebaseAuth,
				"test-project"
		).verify(ID_TOKEN, true))
				.isInstanceOfSatisfying(
						FirebaseAdminClientException.class,
						exception -> assertThat(exception.reason()).isEqualTo(expectedReason)
				)
				.hasMessage("Firebase Admin verification failed.")
				.hasMessageNotContaining(sdkException.getMessage())
				.hasMessageNotContaining(ID_TOKEN);
	}

	private void assertClientFailure(
			AbstractFirebaseAuth firebaseAuth,
			FirebaseAdminClientException.Reason expectedReason
	) {
		assertThatThrownBy(() -> new FirebaseSdkAdminClient(
				firebaseAuth,
				"test-project"
		).verify(ID_TOKEN, true))
				.isInstanceOfSatisfying(
						FirebaseAdminClientException.class,
						exception -> assertThat(exception.reason()).isEqualTo(expectedReason)
				)
				.hasMessage("Firebase Admin verification failed.")
				.hasMessageNotContaining(ID_TOKEN)
				.hasMessageNotContaining("unexpected internal detail");
	}

	private UserInfo provider(String providerId, String providerUid) {
		UserInfo provider = mock(UserInfo.class);
		when(provider.getProviderId()).thenReturn(providerId);
		when(provider.getUid()).thenReturn(providerUid);
		return provider;
	}
}
