package web.tosunsaeng.identity.domain.user.application;

import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

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
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		}
		return UserProfileResponse.from(user);
	}
}
