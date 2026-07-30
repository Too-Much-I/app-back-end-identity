package web.tosunsaeng.identity.domain.auth.application;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@Service
public class SignupService {

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;
	private final UserFactory userFactory;

	public SignupService(
			UserRepository userRepository,
			EmailNormalizer emailNormalizer,
			UserFactory userFactory
	) {
		this.userRepository = userRepository;
		this.emailNormalizer = emailNormalizer;
		this.userFactory = userFactory;
	}

	public SignupResponse signup(SignupRequest request) {
		String normalizedEmail = emailNormalizer.normalize(request.email());
		if (userRepository.existsByNormalizedEmail(normalizedEmail)) {
			throw new AuthException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}

		if (!Boolean.TRUE.equals(request.isAudioConsent())) {
			throw new AuthException(AuthErrorStatus.AUDIO_CONSENT_REQUIRED);
		}

		User user = userFactory.create(
				request.email(),
				request.password(),
				request.nickname().trim()
		);

		// 사전 중복 검사 이후의 동시 가입은 MongoDB 고유 인덱스로 다시 차단한다.
		try {
			User savedUser = userRepository.save(user);
			return SignupResponse.from(savedUser);
		} catch (DuplicateKeyException exception) {
			throw new AuthException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}
	}
}
