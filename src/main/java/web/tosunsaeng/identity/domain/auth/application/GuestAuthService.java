package web.tosunsaeng.identity.domain.auth.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.dto.request.GuestAuthRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.guest.GuestInstallationIdHasher;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

@Service
@RequiredArgsConstructor
public class GuestAuthService {

	private final UserRepository userRepository;
	private final UserFactory userFactory;
	private final ConsentPolicy consentPolicy;
	private final GuestInstallationIdHasher installationIdHasher;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final GuestRegistrationTransactionService registrationTransactionService;
	private final Clock clock;

	public GuestAuthResponse authenticate(GuestAuthRequest request) {
		GuestAuthRequest requiredRequest = Objects.requireNonNull(
				request,
				"request must not be null"
		);
		consentPolicy.validate(
				requiredRequest.isPrivacyConsented(),
				requiredRequest.privacyConsentVersion(),
				requiredRequest.isTermConsented(),
				requiredRequest.termConsentVersion()
		);

		String installationIdHash = installationIdHasher.hash(
				requiredRequest.installationId()
		);
		if (userRepository.existsByGuestInstallationIdHash(installationIdHash)) {
			throw new AuthException(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		}

		User guestUser = userFactory.createGuest(installationIdHash, clock.instant());
		// 영속성 전에 두 Token과 RefreshSession 문서를 모두 준비한다.
		IssuedAccessToken accessToken = accessTokenIssuer.issue(
				guestUser.getUserId(),
				Set.of()
		);
		PreparedRefreshSession refreshSession = refreshSessionIssuer.prepare(
				guestUser.getUserId()
		);
		try {
			return registrationTransactionService.register(
					guestUser,
					accessToken,
					refreshSession
			);
		} catch (DuplicateKeyException exception) {
			// Transaction rollback 뒤 현재 hash가 존재할 때만 설치 중복으로 분류한다.
			if (userRepository.existsByGuestInstallationIdHash(installationIdHash)) {
				throw new AuthException(AuthErrorStatus.GUEST_ALREADY_EXISTS);
			}
			throw exception;
		}
	}
}
