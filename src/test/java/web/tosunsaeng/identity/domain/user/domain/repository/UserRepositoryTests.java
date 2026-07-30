package web.tosunsaeng.identity.domain.user.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.repository.MongoRepository;

class UserRepositoryTests {

	@Test
	void exposesNormalizedEmailLookupContractWithoutMongoConnection() {
		UserRepository userRepository = mock(UserRepository.class);
		String normalizedEmail = "sample.user@example.test";

		when(userRepository.findByNormalizedEmail(normalizedEmail)).thenReturn(Optional.empty());
		when(userRepository.existsByNormalizedEmail(normalizedEmail)).thenReturn(false);

		assertThat(userRepository.findByNormalizedEmail(normalizedEmail)).isEmpty();
		assertThat(userRepository.existsByNormalizedEmail(normalizedEmail)).isFalse();
		assertThat(MongoRepository.class).isAssignableFrom(UserRepository.class);
		verify(userRepository).findByNormalizedEmail(normalizedEmail);
		verify(userRepository).existsByNormalizedEmail(normalizedEmail);
	}
}
