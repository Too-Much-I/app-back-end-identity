package web.tosunsaeng.identity.auth.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.user.domain.User;
import web.tosunsaeng.identity.user.domain.UserFactory;
import web.tosunsaeng.identity.user.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;
	private final UserFactory userFactory;

	public AuthService(
			UserRepository userRepository,
			EmailNormalizer emailNormalizer,
			UserFactory userFactory
	) {
		this.userRepository = userRepository;
		this.emailNormalizer = emailNormalizer;
		this.userFactory = userFactory;
	}

	public CheckEmailResponse checkEmail(String email) {
		String normalizedEmail = emailNormalizer.normalize(email);
		boolean exists = userRepository.existsByNormalizedEmail(normalizedEmail);
		return CheckEmailResponse.from(!exists);
	}

	public SignupResponse signup(SignupRequest request) {
		String normalizedEmail = emailNormalizer.normalize(request.email());
		if (userRepository.existsByNormalizedEmail(normalizedEmail)) {
			throw new BusinessException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}

		if (!Boolean.TRUE.equals(request.isAudioConsent())) {
			throw new BusinessException(AuthErrorStatus.AUDIO_CONSENT_REQUIRED);
		}

		User user = userFactory.create(
				request.email(),
				request.password(),
				request.nickname().trim()
		);

		try {
			User savedUser = userRepository.save(user);
			return SignupResponse.from(savedUser);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}
	}
}
