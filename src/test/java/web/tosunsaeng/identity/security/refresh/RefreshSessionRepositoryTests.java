package web.tosunsaeng.identity.security.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.repository.MongoRepository;

class RefreshSessionRepositoryTests {

	@Test
	void exposesOnlyHashLookupContractsWithoutMongoConnection() {
		RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
		String tokenHash = "test-token-hash";
		when(repository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());
		when(repository.findAllByUserIdAndRevokedAtIsNull("test-user-id"))
				.thenReturn(List.of());
		when(repository.existsByTokenHash(tokenHash)).thenReturn(false);

		assertThat(repository.findByTokenHash(tokenHash)).isEmpty();
		assertThat(repository.findAllByUserIdAndRevokedAtIsNull("test-user-id")).isEmpty();
		assertThat(repository.existsByTokenHash(tokenHash)).isFalse();
		assertThat(MongoRepository.class).isAssignableFrom(RefreshSessionRepository.class);
		verify(repository).findByTokenHash(tokenHash);
		verify(repository).findAllByUserIdAndRevokedAtIsNull("test-user-id");
		verify(repository).existsByTokenHash(tokenHash);
	}
}
