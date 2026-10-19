package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AbstractFirebaseAuth;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserRecord;

import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseAccountPresence;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseAccountSnapshot;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseCleanupException;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseLinkedProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseUserCleanupPort;

public final class FirebaseSdkAbandonedEnrollmentCleanupAdapter
		implements AbandonedFirebaseUserCleanupPort {

	private static final String GOOGLE = "google.com";
	private static final String APPLE = "apple.com";
	private final AbstractFirebaseAuth firebaseAuth;
	private final String projectId;
	private final String kakaoProviderId;

	public FirebaseSdkAbandonedEnrollmentCleanupAdapter(
			FirebaseApp firebaseApp,
			FirebaseAuthProperties properties
	) {
		this(select(firebaseApp, properties.tenantId()), properties.projectId(),
				properties.kakaoProviderId());
	}

	FirebaseSdkAbandonedEnrollmentCleanupAdapter(
			AbstractFirebaseAuth firebaseAuth,
			String projectId,
			String kakaoProviderId
	) {
		this.firebaseAuth = Objects.requireNonNull(firebaseAuth);
		this.projectId = requireText(projectId, "projectId");
		this.kakaoProviderId = requireText(kakaoProviderId, "kakaoProviderId");
	}

	@Override
	public AbandonedFirebaseAccountSnapshot inspect(String projectId, String firebaseUid) {
		guard(projectId, firebaseUid);
		try {
			UserRecord user = firebaseAuth.getUser(firebaseUid);
			return new AbandonedFirebaseAccountSnapshot(
					user.isDisabled(), user.getPhoneNumber(), linkedProviders(user.getProviderData())
			);
		} catch (FirebaseAuthException exception) {
			throw classify(exception);
		} catch (RuntimeException exception) {
			throw failed(AbandonedFirebaseEnrollmentFailureCode.RESULT_UNKNOWN);
		}
	}

	@Override
	public void disable(String projectId, String firebaseUid) {
		guard(projectId, firebaseUid);
		try {
			firebaseAuth.updateUser(new UserRecord.UpdateRequest(firebaseUid).setDisabled(true));
		} catch (FirebaseAuthException exception) {
			throw classify(exception);
		} catch (RuntimeException exception) {
			throw failed(AbandonedFirebaseEnrollmentFailureCode.RESULT_UNKNOWN);
		}
	}

	@Override
	public void revokeRefreshTokens(String projectId, String firebaseUid) {
		guard(projectId, firebaseUid);
		try {
			firebaseAuth.revokeRefreshTokens(firebaseUid);
		} catch (FirebaseAuthException exception) {
			throw classify(exception);
		} catch (RuntimeException exception) {
			throw failed(AbandonedFirebaseEnrollmentFailureCode.RESULT_UNKNOWN);
		}
	}

	@Override
	public void satisfyProviderDeletionObligations(AbandonedFirebaseAccountSnapshot snapshot) {
		if (Objects.requireNonNull(snapshot).linkedProviders().stream()
				.anyMatch(provider -> provider.provider() == SocialProvider.APPLE)) {
			throw failed(AbandonedFirebaseEnrollmentFailureCode.PROVIDER_OBLIGATION_REQUIRED);
		}
	}

	@Override
	public void delete(String projectId, String firebaseUid) {
		guard(projectId, firebaseUid);
		try {
			firebaseAuth.deleteUser(firebaseUid);
		} catch (FirebaseAuthException exception) {
			throw classify(exception);
		} catch (RuntimeException exception) {
			throw failed(AbandonedFirebaseEnrollmentFailureCode.RESULT_UNKNOWN);
		}
	}

	@Override
	public AbandonedFirebaseAccountPresence checkPresence(String projectId, String firebaseUid) {
		guard(projectId, firebaseUid);
		try {
			firebaseAuth.getUser(firebaseUid);
			return AbandonedFirebaseAccountPresence.PRESENT;
		} catch (FirebaseAuthException exception) {
			if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
				return AbandonedFirebaseAccountPresence.ABSENT;
			}
			throw classify(exception);
		} catch (RuntimeException exception) {
			throw failed(AbandonedFirebaseEnrollmentFailureCode.RESULT_UNKNOWN);
		}
	}

	private List<AbandonedFirebaseLinkedProvider> linkedProviders(UserInfo[] providerData) {
		List<AbandonedFirebaseLinkedProvider> result = new ArrayList<>();
		if (providerData == null) return result;
		for (UserInfo info : providerData) {
			if (info == null || info.getUid() == null || info.getUid().isBlank()) continue;
			SocialProvider provider = mapProvider(info.getProviderId());
			if (provider != null) {
				result.add(new AbandonedFirebaseLinkedProvider(provider, info.getUid()));
			}
		}
		return result;
	}

	private SocialProvider mapProvider(String providerId) {
		if (GOOGLE.equals(providerId)) return SocialProvider.GOOGLE;
		if (APPLE.equals(providerId)) return SocialProvider.APPLE;
		if (kakaoProviderId.equals(providerId)) return SocialProvider.KAKAO;
		return null;
	}

	private void guard(String requestedProjectId, String firebaseUid) {
		if (!projectId.equals(requireText(requestedProjectId, "projectId"))) {
			throw failed(AbandonedFirebaseEnrollmentFailureCode.PROJECT_MISMATCH);
		}
		requireText(firebaseUid, "firebaseUid");
	}

	private static AbandonedFirebaseCleanupException classify(FirebaseAuthException exception) {
		if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
			return failed(AbandonedFirebaseEnrollmentFailureCode.NOT_FOUND);
		}
		if (exception.getAuthErrorCode() == AuthErrorCode.CONFIGURATION_NOT_FOUND
				|| exception.getAuthErrorCode() == AuthErrorCode.TENANT_NOT_FOUND
				|| exception.getAuthErrorCode() == AuthErrorCode.TENANT_ID_MISMATCH) {
			return failed(AbandonedFirebaseEnrollmentFailureCode.CONFIGURATION_ERROR);
		}
		ErrorCode code = exception.getErrorCode();
		if (code == ErrorCode.RESOURCE_EXHAUSTED) {
			return failed(AbandonedFirebaseEnrollmentFailureCode.RATE_LIMITED);
		}
		if (code == ErrorCode.DEADLINE_EXCEEDED) {
			return failed(AbandonedFirebaseEnrollmentFailureCode.TIMEOUT);
		}
		if (code == ErrorCode.UNAVAILABLE || code == ErrorCode.INTERNAL
				|| code == ErrorCode.UNKNOWN) {
			return failed(AbandonedFirebaseEnrollmentFailureCode.UNAVAILABLE);
		}
		if (code == ErrorCode.PERMISSION_DENIED || code == ErrorCode.UNAUTHENTICATED) {
			return failed(AbandonedFirebaseEnrollmentFailureCode.PERMISSION_DENIED);
		}
		return failed(AbandonedFirebaseEnrollmentFailureCode.RESULT_UNKNOWN);
	}

	private static AbstractFirebaseAuth select(FirebaseApp app, String tenantId) {
		FirebaseAuth auth = FirebaseAuth.getInstance(Objects.requireNonNull(app));
		return tenantId == null ? auth : auth.getTenantManager().getAuthForTenant(tenantId);
	}

	private static AbandonedFirebaseCleanupException failed(
			AbandonedFirebaseEnrollmentFailureCode code
	) {
		return new AbandonedFirebaseCleanupException(code);
	}

	private static String requireText(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
		return required;
	}
}
