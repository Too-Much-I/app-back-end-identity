package web.tosunsaeng.identity.domain.auth.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class SignupService {

	private static final Logger log = LoggerFactory.getLogger(SignupService.class);

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;
	private final UserFactory userFactory;
	private final ConsentPolicy consentPolicy;

	public SignupResponse signup(SignupRequest request) {
		String normalizedEmail = emailNormalizer.normalize(request.email());
		if (userRepository.existsByNormalizedEmail(normalizedEmail)) {
			throw new AuthException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}

		consentPolicy.validate(
				request.isPrivacyConsented(),
				request.privacyConsentVersion(),
				request.isTermConsented(),
				request.termConsentVersion()
		);

		User user = userFactory.create(
				request.email(),
				request.password(),
				request.nickname().trim()
		);

		// 사전 중복 검사 이후의 동시 가입은 MongoDB 고유 인덱스로 다시 차단한다.
		try {
			User savedUser = userRepository.save(user);
			SignupResponse response = SignupResponse.from(savedUser);
			log.atInfo()
					.addKeyValue("event", "identity.user.registered")
					.addKeyValue("outcome", "created")
					.addKeyValue("userId", savedUser.getUserId())
					.addKeyValue("accountType", savedUser.getAccountType())
					.addKeyValue("provider", savedUser.getProvider())
					.log("이메일 회원가입이 완료되었습니다");
			return response;
		} catch (DuplicateKeyException exception) {
			throw new AuthException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}
	}
}
