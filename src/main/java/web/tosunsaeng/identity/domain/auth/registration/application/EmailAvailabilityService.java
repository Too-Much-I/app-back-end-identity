package web.tosunsaeng.identity.domain.auth.registration.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.registration.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class EmailAvailabilityService {

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;

	public CheckEmailResponse checkEmail(String email) {
		String normalizedEmail = emailNormalizer.normalize(email);
		boolean exists = userRepository.existsByNormalizedEmail(normalizedEmail);
		return CheckEmailResponse.from(!exists);
	}
}
