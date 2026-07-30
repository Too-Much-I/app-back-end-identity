package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;

class RefreshSessionTests {

	@Test
	void declaresCollectionIdentifierUniqueHashAndTtlIndexes() throws NoSuchFieldException {
		Document document = RefreshSession.class.getAnnotation(Document.class);
		Field sessionId = RefreshSession.class.getDeclaredField("sessionId");
		Field version = RefreshSession.class.getDeclaredField("version");
		Indexed tokenHashIndex = RefreshSession.class
				.getDeclaredField("tokenHash")
				.getAnnotation(Indexed.class);
		Indexed expiresAtIndex = RefreshSession.class
				.getDeclaredField("expiresAt")
				.getAnnotation(Indexed.class);

		assertThat(document.collection()).isEqualTo("refresh_sessions");
		assertThat(sessionId.isAnnotationPresent(Id.class)).isTrue();
		assertThat(version.isAnnotationPresent(Version.class)).isTrue();
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
						"rotationFamilyId",
						"rotatedFromSessionId",
						"replacedBySessionId",
						"revocationReason",
						"version"
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
		assertThat(session.getRotationFamilyId()).isNotNull();
		assertThat(session.getReplacedBySessionId()).isNull();
		assertThat(session.getRevocationReason()).isNull();
		assertThat(session.getRotatedFromSessionId()).isNull();
		assertThat(session.getVersion()).isNull();
	}

	@Test
	void rotatesSessionAndCreatesLinkedSuccessorInSameFamily() {
		Instant createdAt = Instant.parse("2026-07-24T09:10:11Z");
		Instant rotatedAt = createdAt.plusSeconds(30);
		RefreshSession current = RefreshSession.create(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				"current-test-token-hash",
				createdAt,
				createdAt.plus(Duration.ofDays(14))
		);
		String successorId = RefreshSession.newSessionId();

		current.rotate(rotatedAt, successorId);
		RefreshSession successor = RefreshSession.createRotated(
				successorId,
				current.getUserId(),
				current.getRotationFamilyId(),
				current.getSessionId(),
				"successor-test-token-hash",
				rotatedAt,
				rotatedAt.plus(Duration.ofDays(14))
		);

		assertThat(current.getRevokedAt()).isEqualTo(rotatedAt);
		assertThat(current.getLastUsedAt()).isEqualTo(rotatedAt);
		assertThat(current.getRevocationReason()).isEqualTo(RevocationReason.ROTATED);
		assertThat(current.getReplacedBySessionId()).isEqualTo(successor.getSessionId());
		assertThat(successor.getRotationFamilyId()).isEqualTo(current.getRotationFamilyId());
		assertThat(successor.getRotatedFromSessionId()).isEqualTo(current.getSessionId());
		assertThat(successor.isRevoked()).isFalse();
	}

	@Test
	void considersExpirationInstantExpiredAndSupportsLogoutAndReuseReasons() {
		Instant createdAt = Instant.parse("2026-07-24T09:10:11Z");
		Instant expiresAt = createdAt.plusSeconds(60);
		RefreshSession logoutSession = RefreshSession.create(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				"logout-test-token-hash",
				createdAt,
				expiresAt
		);
		RefreshSession reusedFamilySession = RefreshSession.create(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				"reuse-test-token-hash",
				createdAt,
				expiresAt
		);
		RefreshSession logoutAllSession = RefreshSession.create(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				"logout-all-test-token-hash",
				createdAt,
				expiresAt
		);

		assertThat(logoutSession.isExpiredAt(expiresAt.minusNanos(1))).isFalse();
		assertThat(logoutSession.isExpiredAt(expiresAt)).isTrue();
		logoutSession.logout(createdAt.plusSeconds(10));
		reusedFamilySession.revokeForReuse(createdAt.plusSeconds(20));
		logoutAllSession.logoutAll(createdAt.plusSeconds(30));

		assertThat(logoutSession.getRevocationReason()).isEqualTo(RevocationReason.LOGOUT);
		assertThat(reusedFamilySession.getRevocationReason())
				.isEqualTo(RevocationReason.REUSE_DETECTED);
		assertThat(logoutAllSession.getRevokedAt()).isEqualTo(createdAt.plusSeconds(30));
		assertThat(logoutAllSession.getLastUsedAt()).isEqualTo(createdAt.plusSeconds(30));
		assertThat(logoutAllSession.getRevocationReason())
				.isEqualTo(RevocationReason.LOGOUT_ALL);
	}

	@Test
	void initializesFamilyForSessionCreatedBeforeRotationFamilyFieldWasIntroduced() {
		Instant createdAt = Instant.parse("2026-07-24T09:10:11Z");
		RefreshSession session = RefreshSession.create(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				"legacy-session-test-hash",
				createdAt,
				createdAt.plus(Duration.ofDays(14))
		);
		ReflectionTestUtils.setField(session, "rotationFamilyId", null);

		String initializedFamilyId = session.initializeRotationFamilyIfMissing();

		assertThat(initializedFamilyId).isEqualTo(session.getRotationFamilyId());
		assertThat(initializedFamilyId).isNotBlank();
	}
}
