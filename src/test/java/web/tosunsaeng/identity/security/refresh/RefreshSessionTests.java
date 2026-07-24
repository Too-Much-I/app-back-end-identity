package web.tosunsaeng.identity.security.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

class RefreshSessionTests {

	@Test
	void declaresCollectionIdentifierUniqueHashAndTtlIndexes() throws NoSuchFieldException {
		Document document = RefreshSession.class.getAnnotation(Document.class);
		Field sessionId = RefreshSession.class.getDeclaredField("sessionId");
		Indexed tokenHashIndex = RefreshSession.class
				.getDeclaredField("tokenHash")
				.getAnnotation(Indexed.class);
		Indexed expiresAtIndex = RefreshSession.class
				.getDeclaredField("expiresAt")
				.getAnnotation(Indexed.class);

		assertThat(document.collection()).isEqualTo("refresh_sessions");
		assertThat(sessionId.isAnnotationPresent(Id.class)).isTrue();
		assertThat(tokenHashIndex.unique()).isTrue();
		assertThat(tokenHashIndex.name()).isEqualTo("uk_refresh_sessions_token_hash");
		assertThat(expiresAtIndex.name()).isEqualTo("ttl_refresh_sessions_expires_at");
		assertThat(expiresAtIndex.expireAfter()).isEqualTo("0s");
	}

	@Test
	void containsOnlyHashAndSessionMetadataWithoutRawRefreshOrAccessTokenFields() {
		assertThat(Arrays.stream(RefreshSession.class.getDeclaredFields()).map(Field::getName))
				.contains(
						"sessionId",
						"userId",
						"tokenHash",
						"createdAt",
						"expiresAt",
						"lastUsedAt",
						"revokedAt",
						"rotatedFromSessionId"
				)
				.doesNotContain(
						"refreshToken",
						"token",
						"tokenValue",
						"accessToken",
						"email",
						"password",
						"passwordHash"
				);
	}

	@Test
	void createsInitialSessionWithExpectedNullAndTimestampFields() {
		Instant createdAt = Instant.parse("2026-07-24T09:10:11Z");
		RefreshSession session = RefreshSession.create(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				"test-token-hash",
				createdAt,
				createdAt.plus(Duration.ofDays(14))
		);

		assertThat(session.getCreatedAt()).isEqualTo(createdAt);
		assertThat(session.getLastUsedAt()).isEqualTo(createdAt);
		assertThat(session.getRevokedAt()).isNull();
		assertThat(session.getRotatedFromSessionId()).isNull();
	}
}
