package web.tosunsaeng.identity.user.service;

import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.user.domain.User;
import web.tosunsaeng.identity.user.domain.UserStatus;
import web.tosunsaeng.identity.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.user.repository.UserRepository;

@Service
public class UserProfileService {

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;

	public UserProfileService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository
	) {
		this.currentUserProvider = currentUserProvider;
		this.userRepository = userRepository;
	}

	public UserProfileResponse getCurrentUserProfile() {
		String userId = currentUserProvider.getCurrentUserId();
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorStatus.ACCOUNT_NOT_ACTIVE);
		}
		return UserProfileResponse.from(user);
	}
}
