package web.tosunsaeng.identity.domain.auth.registration.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

@Service
@RequiredArgsConstructor
public class GuestRegistrationTransactionService {

	private final UserRepository userRepository;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final AuthResponseConverter authResponseConverter;

	@Transactional(transactionManager = "mongoTransactionManager")
	public GuestAuthResponse register(
			User guestUser,
			IssuedAccessToken accessToken,
			PreparedRefreshSession preparedRefreshSession
	) {
		if (!guestUser.getUserId().equals(preparedRefreshSession.session().getUserId())) {
			throw new IllegalArgumentException("Guest and RefreshSession user IDs must match.");
		}

		userRepository.save(guestUser);
		IssuedRefreshSession refreshSession = refreshSessionIssuer.savePrepared(
				preparedRefreshSession
		);
		// 응답 구성까지 Transaction 안에서 끝내 내부 실패 시 두 문서를 함께 rollback한다.
		return authResponseConverter.toGuestAuthResponse(
				accessToken,
				refreshSession.tokenValue(),
				refreshSession.issuedAt(),
				refreshSession.expiresAt()
		);
	}
}
