package web.tosunsaeng.identity.security.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshSessionIssuerTests {

	private static final Instant NOW = Instant.parse("2026-07-24T07:08:09Z");
	private static final Duration TTL = Duration.ofDays(14);
	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";

	@Test
	void hashesOpaqueTokenAndStoresInitialSessionUsingInjectedClock() {
		RefreshTokenGenerator tokenGenerator = mock(RefreshTokenGenerator.class);
		RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
		RefreshTokenHasher tokenHasher = new RefreshTokenHasher();
		RefreshTokenProperties properties = new RefreshTokenProperties(TTL, 32);
		when(tokenGenerator.generate()).thenReturn("opaque-test-value");
		when(repository.save(any(RefreshSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		RefreshSessionIssuer issuer = new RefreshSessionIssuer(
				tokenGenerator,
				tokenHasher,
				repository,
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		IssuedRefreshSession issuedSession = issuer.issue(USER_ID);

		ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
		verify(repository).save(sessionCaptor.capture());
		RefreshSession savedSession = sessionCaptor.getValue();
		assertThat(savedSession.getUserId()).isEqualTo(USER_ID);
		assertThat(savedSession.getTokenHash())
				.isEqualTo(tokenHasher.hash(issuedSession.tokenValue()))
				.isNotEqualTo(issuedSession.tokenValue());
		assertThat(savedSession.getCreatedAt()).isEqualTo(NOW);
		assertThat(savedSession.getLastUsedAt()).isEqualTo(NOW);
		assertThat(savedSession.getExpiresAt()).isEqualTo(NOW.plus(TTL));
		assertThat(savedSession.getRevokedAt()).isNull();
		assertThat(UUID.fromString(savedSession.getRotationFamilyId()).toString())
				.isEqualTo(savedSession.getRotationFamilyId());
		assertThat(savedSession.getReplacedBySessionId()).isNull();
		assertThat(savedSession.getRevocationReason()).isNull();
		assertThat(savedSession.getRotatedFromSessionId()).isNull();
		assertThat(issuedSession.expiresAt()).isEqualTo(savedSession.getExpiresAt());
		assertThat(issuedSession.toString())
				.contains("tokenValue=redacted")
				.doesNotContain(issuedSession.tokenValue());
	}

	@Test
	void hashesAndStoresRotatedSessionWithReservedIdentityAndFamily() {
		RefreshTokenGenerator tokenGenerator = mock(RefreshTokenGenerator.class);
		RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
		RefreshTokenHasher tokenHasher = new RefreshTokenHasher();
		RefreshTokenProperties properties = new RefreshTokenProperties(TTL, 32);
		String sessionId = "056ec510-0f4d-401a-9228-668995e62140";
		String familyId = "9409fcd9-4413-46c4-b2f5-30639588b892";
		String previousSessionId = "db074c2b-c7b4-4d35-8258-df777340f16c";
		when(tokenGenerator.generate()).thenReturn("rotated-opaque-test-value");
		when(repository.save(any(RefreshSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		RefreshSessionIssuer issuer = new RefreshSessionIssuer(
				tokenGenerator,
				tokenHasher,
				repository,
				properties,
				Clock.fixed(NOW.plusSeconds(30), ZoneOffset.UTC)
		);

		IssuedRefreshSession issuedSession = issuer.issueRotated(
				sessionId,
				USER_ID,
				familyId,
				previousSessionId,
				NOW
		);

		ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
		verify(repository).save(sessionCaptor.capture());
		RefreshSession savedSession = sessionCaptor.getValue();
		assertThat(savedSession.getSessionId()).isEqualTo(sessionId);
		assertThat(savedSession.getUserId()).isEqualTo(USER_ID);
		assertThat(savedSession.getRotationFamilyId()).isEqualTo(familyId);
		assertThat(savedSession.getRotatedFromSessionId()).isEqualTo(previousSessionId);
		assertThat(savedSession.getTokenHash())
				.isEqualTo(tokenHasher.hash(issuedSession.tokenValue()))
				.isNotEqualTo(issuedSession.tokenValue());
		assertThat(savedSession.getCreatedAt()).isEqualTo(NOW);
		assertThat(savedSession.getLastUsedAt()).isEqualTo(NOW);
		assertThat(savedSession.getExpiresAt()).isEqualTo(NOW.plus(TTL));
		assertThat(savedSession.getRevokedAt()).isNull();
		assertThat(savedSession.getReplacedBySessionId()).isNull();
		assertThat(savedSession.getRevocationReason()).isNull();
	}
}
