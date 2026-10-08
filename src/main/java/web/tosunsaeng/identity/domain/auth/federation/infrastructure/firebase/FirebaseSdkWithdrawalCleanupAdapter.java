package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AbstractFirebaseAuth;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserRecord;

import web.tosunsaeng.identity.domain.user.application.FirebaseAccountPresence;
import web.tosunsaeng.identity.domain.user.application.FirebaseCleanupAccountSnapshot;
import web.tosunsaeng.identity.domain.user.application.FirebaseCleanupProvider;
import web.tosunsaeng.identity.domain.user.application.FirebaseWithdrawalCleanupException;
import web.tosunsaeng.identity.domain.user.application.FirebaseWithdrawalCleanupPort;
import web.tosunsaeng.identity.domain.user.application.ProviderObligationResult;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;

public final class FirebaseSdkWithdrawalCleanupAdapter
		implements FirebaseWithdrawalCleanupPort {

	private static final Logger log = LoggerFactory.getLogger(FirebaseSdkWithdrawalCleanupAdapter.class);
	private enum Operation { INSPECT, DISABLE, REVOKE_REFRESH_TOKENS, DELETE, CHECK_PRESENCE }

	private static final String GOOGLE_PROVIDER_ID = "google.com";
	private static final String APPLE_PROVIDER_ID = "apple.com";

	private final AbstractFirebaseAuth firebaseAuth;
	private final String configuredProjectId;
	private final String kakaoProviderId;

	public FirebaseSdkWithdrawalCleanupAdapter(
			FirebaseApp firebaseApp,
			FirebaseAuthProperties properties
	) {
		this(
				selectFirebaseAuth(firebaseApp, properties.tenantId()),
				properties.projectId(),
				properties.kakaoProviderId()
		);
	}

	FirebaseSdkWithdrawalCleanupAdapter(
			AbstractFirebaseAuth firebaseAuth,
			String configuredProjectId,
			String kakaoProviderId
	) {
		this.firebaseAuth = Objects.requireNonNull(firebaseAuth, "firebaseAuth must not be null");
		this.configuredProjectId = requireText(configuredProjectId, "configuredProjectId");
		this.kakaoProviderId = requireText(kakaoProviderId, "kakaoProviderId");
	}

	@Override
	public FirebaseCleanupAccountSnapshot inspect(String projectId, String firebaseUid) {
		guardTarget(projectId, firebaseUid);
		try {
			UserRecord user = firebaseAuth.getUser(firebaseUid);
			return new FirebaseCleanupAccountSnapshot(
					user.isDisabled(),
					providers(user.getProviderData())
			);
		} catch (FirebaseAuthException exception) {
			throw diagnose(Operation.INSPECT, exception);
		} catch (RuntimeException exception) {
			throw diagnose(Operation.INSPECT, exception);
		}
	}

	@Override
	public void disable(String projectId, String firebaseUid) {
		guardTarget(projectId, firebaseUid);
		try {
			firebaseAuth.updateUser(new UserRecord.UpdateRequest(firebaseUid).setDisabled(true));
		} catch (FirebaseAuthException exception) {
			throw diagnose(Operation.DISABLE, exception);
		} catch (RuntimeException exception) {
			throw diagnose(Operation.DISABLE, exception);
		}
	}

	@Override
	public void revokeRefreshTokens(String projectId, String firebaseUid) {
		guardTarget(projectId, firebaseUid);
		try {
			firebaseAuth.revokeRefreshTokens(firebaseUid);
		} catch (FirebaseAuthException exception) {
			throw diagnose(Operation.REVOKE_REFRESH_TOKENS, exception);
		} catch (RuntimeException exception) {
			throw diagnose(Operation.REVOKE_REFRESH_TOKENS, exception);
		}
	}

	@Override
	public ProviderObligationResult satisfyProviderDeletionObligations(
			FirebaseCleanupAccountSnapshot snapshot
	) {
		FirebaseCleanupAccountSnapshot required = Objects.requireNonNull(
				snapshot,
				"snapshot must not be null"
		);
		if (required.providers().contains(FirebaseCleanupProvider.APPLE)) {
			throw failed(WithdrawalCleanupFailureCode.PROVIDER_OBLIGATION_REQUIRED);
		}
		return ProviderObligationResult.NOT_REQUIRED;
	}

	@Override
	public void delete(String projectId, String firebaseUid) {
		guardTarget(projectId, firebaseUid);
		try {
			firebaseAuth.deleteUser(firebaseUid);
		} catch (FirebaseAuthException exception) {
			throw diagnose(Operation.DELETE, exception);
		} catch (RuntimeException exception) {
			throw diagnose(Operation.DELETE, exception);
		}
	}

	@Override
	public FirebaseAccountPresence checkPresence(String projectId, String firebaseUid) {
		guardTarget(projectId, firebaseUid);
		try {
			firebaseAuth.getUser(firebaseUid);
			return FirebaseAccountPresence.PRESENT;
		} catch (FirebaseAuthException exception) {
			if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
				return FirebaseAccountPresence.ABSENT;
			}
			throw diagnose(Operation.CHECK_PRESENCE, exception);
		} catch (RuntimeException exception) {
			throw diagnose(Operation.CHECK_PRESENCE, exception);
		}
	}

	private static FirebaseWithdrawalCleanupException diagnose(Operation operation, Exception exception) {
		FirebaseAuthException sdk = exception instanceof FirebaseAuthException firebase ? firebase : null;
		FirebaseWithdrawalCleanupException mapped = sdk == null
				? failed(WithdrawalCleanupFailureCode.RESULT_UNKNOWN) : classify(sdk);
		// USER_NOT_FOUND is an expected idempotent cleanup outcome, not an operational failure.
		if (mapped.failureCode() != WithdrawalCleanupFailureCode.NOT_FOUND) {
			FirebaseCleanupHttpDiagnostic http = FirebaseCleanupHttpDiagnostic.from(
					sdk == null ? null : sdk.getHttpResponse());
			StringBuilder causes = new StringBuilder();
			Throwable cause = exception.getCause();
			for (int depth = 0; cause != null && depth < 5; depth++, cause = cause.getCause()) {
				if (!causes.isEmpty()) causes.append(" <- ");
				causes.append(cause.getClass().getName());
			}
			// Never attach Throwable, its message, HTTP response, UID, project or credentials.
			log.atWarn()
					.addKeyValue("event", "user.withdrawal.firebase_operation_failed")
					.addKeyValue("operation", operation.name())
					.addKeyValue("failureCode", mapped.failureCode().name())
					.addKeyValue("firebaseHttpStatus", http.status())
					.addKeyValue("firebaseRemoteErrorCode", http.remoteCode())
					.addKeyValue("exceptionType", exception.getClass().getName())
					.addKeyValue("causeTypes", causes.isEmpty() ? "NONE" : causes.toString())
					.addKeyValue("firebaseErrorCode", sdk == null || sdk.getErrorCode() == null
							? "NONE" : sdk.getErrorCode().name())
					.addKeyValue("firebaseAuthErrorCode", sdk == null || sdk.getAuthErrorCode() == null
							? "NONE" : sdk.getAuthErrorCode().name())
					.log("Firebase withdrawal operation failed");
		}
		return mapped;
	}

	private Set<FirebaseCleanupProvider> providers(UserInfo[] providerData) {
		EnumSet<FirebaseCleanupProvider> providers = EnumSet.noneOf(FirebaseCleanupProvider.class);
		if (providerData == null) {
			return providers;
		}
		for (UserInfo provider : providerData) {
			if (provider == null) continue;
			String providerId = provider.getProviderId();
			if (GOOGLE_PROVIDER_ID.equals(providerId)) {
				providers.add(FirebaseCleanupProvider.GOOGLE);
			} else if (APPLE_PROVIDER_ID.equals(providerId)) {
				providers.add(FirebaseCleanupProvider.APPLE);
			} else if (kakaoProviderId.equals(providerId)) {
				providers.add(FirebaseCleanupProvider.KAKAO);
			}
		}
		return providers;
	}

	private void guardTarget(String projectId, String firebaseUid) {
		String requiredProjectId = requireText(projectId, "projectId");
		requireText(firebaseUid, "firebaseUid");
		if (!configuredProjectId.equals(requiredProjectId)) {
			throw failed(WithdrawalCleanupFailureCode.PROJECT_MISMATCH);
		}
	}

	private static FirebaseWithdrawalCleanupException classify(
			FirebaseAuthException exception
	) {
		AuthErrorCode authErrorCode = exception.getAuthErrorCode();
		if (authErrorCode == AuthErrorCode.USER_NOT_FOUND) {
			return failed(WithdrawalCleanupFailureCode.NOT_FOUND);
		}
		if (authErrorCode == AuthErrorCode.CONFIGURATION_NOT_FOUND
				|| authErrorCode == AuthErrorCode.TENANT_NOT_FOUND
				|| authErrorCode == AuthErrorCode.TENANT_ID_MISMATCH) {
			return failed(WithdrawalCleanupFailureCode.CONFIGURATION_ERROR);
		}

		ErrorCode errorCode = exception.getErrorCode();
		if (errorCode == ErrorCode.RESOURCE_EXHAUSTED) {
			return failed(WithdrawalCleanupFailureCode.RATE_LIMITED);
		}
		if (errorCode == ErrorCode.DEADLINE_EXCEEDED) {
			return failed(WithdrawalCleanupFailureCode.TIMEOUT);
		}
		if (errorCode == ErrorCode.UNAVAILABLE
				|| errorCode == ErrorCode.INTERNAL
				|| errorCode == ErrorCode.UNKNOWN) {
			return failed(WithdrawalCleanupFailureCode.UNAVAILABLE);
		}
		if (errorCode == ErrorCode.PERMISSION_DENIED
				|| errorCode == ErrorCode.UNAUTHENTICATED) {
			return failed(WithdrawalCleanupFailureCode.PERMISSION_DENIED);
		}
		return failed(WithdrawalCleanupFailureCode.RESULT_UNKNOWN);
	}

	private static AbstractFirebaseAuth selectFirebaseAuth(
			FirebaseApp firebaseApp,
			String tenantId
	) {
		FirebaseAuth auth = FirebaseAuth.getInstance(
				Objects.requireNonNull(firebaseApp, "firebaseApp must not be null")
		);
		return tenantId == null ? auth : auth.getTenantManager().getAuthForTenant(tenantId);
	}

	private static FirebaseWithdrawalCleanupException failed(
			WithdrawalCleanupFailureCode failureCode
	) {
		return new FirebaseWithdrawalCleanupException(failureCode);
	}

	private static String requireText(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return required;
	}
}
