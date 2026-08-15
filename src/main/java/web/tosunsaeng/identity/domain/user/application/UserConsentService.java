package web.tosunsaeng.identity.domain.user.application;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.request.UserConsentUpdateRequest;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentStatusResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

@Service
@RequiredArgsConstructor
public class UserConsentService {

	private static final Logger log = LoggerFactory.getLogger(UserConsentService.class);

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;
	private final ConsentPolicy consentPolicy;
	private final Clock clock;

	public UserConsentStatusResponse getCurrentConsentStatus() {
		User user = getCurrentActiveUser();
		return UserConsentStatusResponse.from(
				user,
				consentPolicy.getPrivacyConsentVersion(),
				consentPolicy.getTermConsentVersion(),
				consentPolicy.getQualityReviewConsentVersion()
		);
	}

	public UserConsentResponse updateConsents(UserConsentUpdateRequest request) {
		consentPolicy.validateWithQualityReview(
				request.isPrivacyConsented(),
				request.privacyConsentVersion(),
				request.isTermConsented(),
				request.termConsentVersion(),
				request.isQualityReviewConsented(),
				request.qualityReviewConsentVersion()
		);

		User user = getCurrentActiveUser();

		Instant consentedAt = clock.instant();
		Instant expectedUpdatedAt = user.getUpdatedAt();
		boolean changed = user.updateConsents(
				consentPolicy.getPrivacyConsentVersion(),
				consentPolicy.getTermConsentVersion(),
				Boolean.TRUE.equals(request.isQualityReviewConsented()),
				consentPolicy.getQualityReviewConsentVersion(),
				consentedAt
		);
		if (!changed) {
			log.atDebug()
					.addKeyValue("event", "user.consents.updated")
					.addKeyValue("outcome", "unchanged")
					.addKeyValue("userId", user.getUserId())
					.addKeyValue("provider", user.getProvider())
					.log("사용자 동의가 이미 최신 상태입니다");
			return UserConsentResponse.from(user);
		}
		if (!userRepository.updateConsentsIfActive(user, expectedUpdatedAt)) {
			User latestUser = userRepository.findById(user.getUserId())
					.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
			if (latestUser.getStatus() != UserStatus.ACTIVE) {
				throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
			}
			throw new UserException(UserErrorStatus.USER_UPDATE_CONFLICT);
		}
		UserConsentResponse response = UserConsentResponse.from(user);
		log.atInfo()
				.addKeyValue("event", "user.consents.updated")
				.addKeyValue("outcome", "updated")
				.addKeyValue("userId", user.getUserId())
				.addKeyValue("provider", user.getProvider())
				.log("사용자 동의를 갱신했습니다");
		return response;
	}

	private User getCurrentActiveUser() {
		String userId = currentUserProvider.getCurrentUserId();
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		}
		return user;
	}
}
