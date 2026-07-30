package web.tosunsaeng.identity.domain.auth.application;

import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@Service
public class EmailAvailabilityService {

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;

	public EmailAvailabilityService(
			UserRepository userRepository,
			EmailNormalizer emailNormalizer
	) {
		this.userRepository = userRepository;
		this.emailNormalizer = emailNormalizer;
	}

	public CheckEmailResponse checkEmail(String email) {
		String normalizedEmail = emailNormalizer.normalize(email);
		boolean exists = userRepository.existsByNormalizedEmail(normalizedEmail);
		return CheckEmailResponse.from(!exists);
	}
}
