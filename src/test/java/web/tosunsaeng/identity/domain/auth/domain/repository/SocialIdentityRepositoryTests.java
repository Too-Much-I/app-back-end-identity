package web.tosunsaeng.identity.domain.auth.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

class SocialIdentityRepositoryTests {

	@Test
	void exposesCanonicalOwnerAndUserIdentityLookupContractsWithoutMongoConnection() {
		SocialIdentityRepository repository = mock(SocialIdentityRepository.class);
		String userId = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
		String providerSubject = "provider-subject";

		when(repository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				providerSubject
		)).thenReturn(Optional.empty());
		when(repository.findAllByUserId(userId)).thenReturn(List.of());

		assertThat(repository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				providerSubject
		)).isEmpty();
		assertThat(repository.findAllByUserId(userId)).isEmpty();
		assertThat(MongoRepository.class).isAssignableFrom(SocialIdentityRepository.class);
		verify(repository).findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				providerSubject
		);
		verify(repository).findAllByUserId(userId);
	}
}
