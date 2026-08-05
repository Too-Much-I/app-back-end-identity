package web.tosunsaeng.identity.domain.user.application;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.request.UserConsentUpdateRequest;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

@Service
@RequiredArgsConstructor
public class UserConsentService {

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;
	private final ConsentPolicy consentPolicy;
	private final Clock clock;

	public UserConsentResponse updateConsents(UserConsentUpdateRequest request) {
		consentPolicy.validate(
				request.isPrivacyConsented(),
				request.privacyConsentVersion(),
				request.isTermConsented(),
				request.termConsentVersion()
		);

		String userId = currentUserProvider.getCurrentUserId();
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		}

		Instant consentedAt = clock.instant();
		boolean changed = user.updateConsents(
				consentPolicy.getPrivacyConsentVersion(),
				consentPolicy.getTermConsentVersion(),
				consentedAt
		);
		User persistedUser = changed ? userRepository.save(user) : user;
		return UserConsentResponse.from(persistedUser);
	}
}
