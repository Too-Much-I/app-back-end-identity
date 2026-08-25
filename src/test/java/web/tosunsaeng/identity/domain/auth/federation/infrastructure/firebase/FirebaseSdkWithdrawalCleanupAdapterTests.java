package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AbstractFirebaseAuth;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.user.application.FirebaseAccountPresence;
import web.tosunsaeng.identity.domain.user.application.FirebaseCleanupAccountSnapshot;
import web.tosunsaeng.identity.domain.user.application.FirebaseCleanupProvider;
import web.tosunsaeng.identity.domain.user.application.FirebaseWithdrawalCleanupException;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;

class FirebaseSdkWithdrawalCleanupAdapterTests {

	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "opaque-uid";

	private AbstractFirebaseAuth firebaseAuth;
	private FirebaseSdkWithdrawalCleanupAdapter adapter;

	@BeforeEach
	void setUp() {
		firebaseAuth = mock(AbstractFirebaseAuth.class);
		adapter = new FirebaseSdkWithdrawalCleanupAdapter(
				firebaseAuth,
				PROJECT_ID,
				"oidc.kakao"
		);
	}

	@Test
	void inspectReturnsOnlySafeStateAndProviderEnums() throws Exception {
		UserRecord user = mock(UserRecord.class);
		UserInfo google = provider("google.com");
		UserInfo kakao = provider("oidc.kakao");
		UserInfo apple = provider("apple.com");
		UserInfo phone = provider("phone");
		when(user.isDisabled()).thenReturn(true);
		when(user.getProviderData()).thenReturn(new UserInfo[]{
				google,
				kakao,
				apple,
				phone
		});
		when(firebaseAuth.getUser(FIREBASE_UID)).thenReturn(user);

		FirebaseCleanupAccountSnapshot snapshot = adapter.inspect(PROJECT_ID, FIREBASE_UID);

		assertThat(snapshot.disabled()).isTrue();
		assertThat(snapshot.providers()).containsExactlyInAnyOrder(
				FirebaseCleanupProvider.GOOGLE,
				FirebaseCleanupProvider.KAKAO,
				FirebaseCleanupProvider.APPLE
		);
		assertThat(snapshot.toString()).doesNotContain(PROJECT_ID, FIREBASE_UID);
	}

	@Test
	void delegatesDisableRevokeDeleteAndPresenceToSdk() throws Exception {
		when(firebaseAuth.updateUser(any(UserRecord.UpdateRequest.class)))
				.thenReturn(mock(UserRecord.class));
		when(firebaseAuth.getUser(FIREBASE_UID)).thenReturn(mock(UserRecord.class));

		adapter.disable(PROJECT_ID, FIREBASE_UID);
		adapter.revokeRefreshTokens(PROJECT_ID, FIREBASE_UID);
		adapter.delete(PROJECT_ID, FIREBASE_UID);

		assertThat(adapter.checkPresence(PROJECT_ID, FIREBASE_UID))
				.isEqualTo(FirebaseAccountPresence.PRESENT);
		verify(firebaseAuth).updateUser(any(UserRecord.UpdateRequest.class));
		verify(firebaseAuth).revokeRefreshTokens(FIREBASE_UID);
		verify(firebaseAuth).deleteUser(FIREBASE_UID);
	}

	@Test
	void projectMismatchBlocksSdkMutation() throws Exception {
		assertFailure(
				() -> adapter.delete("wrong-project", FIREBASE_UID),
				WithdrawalCleanupFailureCode.PROJECT_MISMATCH
		);
		verify(firebaseAuth, never()).deleteUser(any());
	}

	@Test
	void userNotFoundIsIdempotentForPresenceAndSafelyClassifiedElsewhere() throws Exception {
		FirebaseAuthException notFound = sdkFailure(
				ErrorCode.NOT_FOUND,
				AuthErrorCode.USER_NOT_FOUND,
				"internal user detail"
		);
		when(firebaseAuth.getUser(FIREBASE_UID)).thenThrow(notFound);

		assertThat(adapter.checkPresence(PROJECT_ID, FIREBASE_UID))
				.isEqualTo(FirebaseAccountPresence.ABSENT);
		assertFailure(
				() -> adapter.inspect(PROJECT_ID, FIREBASE_UID),
				WithdrawalCleanupFailureCode.NOT_FOUND
		);
	}

	@Test
	void mapsRateTimeoutPermissionAndUnknownWithoutOriginalMessage() throws Exception {
		assertSdkFailure(ErrorCode.RESOURCE_EXHAUSTED, null,
				WithdrawalCleanupFailureCode.RATE_LIMITED);
		assertSdkFailure(ErrorCode.DEADLINE_EXCEEDED, null,
				WithdrawalCleanupFailureCode.TIMEOUT);
		assertSdkFailure(ErrorCode.PERMISSION_DENIED, null,
				WithdrawalCleanupFailureCode.PERMISSION_DENIED);
		assertSdkFailure(ErrorCode.INVALID_ARGUMENT, null,
				WithdrawalCleanupFailureCode.RESULT_UNKNOWN);
	}

	@Test
	void appleProviderRequiresExplicitObligationMaterial() {
		FirebaseCleanupAccountSnapshot snapshot = new FirebaseCleanupAccountSnapshot(
				false,
				Set.of(FirebaseCleanupProvider.APPLE)
		);

		assertFailure(
				() -> adapter.satisfyProviderDeletionObligations(snapshot),
				WithdrawalCleanupFailureCode.PROVIDER_OBLIGATION_REQUIRED
		);
	}

	@Test
	void runtimeFailureIsRedactedAsUnknownResult() throws Exception {
		when(firebaseAuth.getUser(FIREBASE_UID))
				.thenThrow(new IllegalStateException("sensitive internal runtime detail"));

		assertFailure(
				() -> adapter.inspect(PROJECT_ID, FIREBASE_UID),
				WithdrawalCleanupFailureCode.RESULT_UNKNOWN
		);
	}

	private void assertSdkFailure(
			ErrorCode errorCode,
			AuthErrorCode authErrorCode,
			WithdrawalCleanupFailureCode expected
	) throws Exception {
		FirebaseAuthException sdkException = sdkFailure(
				errorCode,
				authErrorCode,
				"sensitive sdk message"
		);
		doThrow(sdkException).when(firebaseAuth).deleteUser(FIREBASE_UID);
		assertFailure(() -> adapter.delete(PROJECT_ID, FIREBASE_UID), expected);
	}

	private void assertFailure(Runnable operation, WithdrawalCleanupFailureCode expected) {
		assertThatThrownBy(operation::run)
				.isInstanceOfSatisfying(
						FirebaseWithdrawalCleanupException.class,
						exception -> assertThat(exception.failureCode()).isEqualTo(expected)
				)
				.hasMessage("Firebase withdrawal cleanup operation failed.")
				.hasMessageNotContaining("sensitive")
				.hasMessageNotContaining(PROJECT_ID)
				.hasMessageNotContaining(FIREBASE_UID);
	}

	private FirebaseAuthException sdkFailure(
			ErrorCode errorCode,
			AuthErrorCode authErrorCode,
			String message
	) {
		return new FirebaseAuthException(errorCode, message, null, null, authErrorCode);
	}

	private UserInfo provider(String providerId) {
		UserInfo provider = mock(UserInfo.class);
		when(provider.getProviderId()).thenReturn(providerId);
		return provider;
	}
}
