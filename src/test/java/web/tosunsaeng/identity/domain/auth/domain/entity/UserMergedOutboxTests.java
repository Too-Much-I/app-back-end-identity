package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedOutboxStatus;

class UserMergedOutboxTests {

	private static final String SOURCE_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String TARGET_ID = "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e";
	private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

	@Test
	void createsVersionOnePendingEventWithOnlyCanonicalMergeIdentity() {
		UserMergedOutbox event = UserMergedOutbox.create(SOURCE_ID, TARGET_ID, NOW);

		assertThat(event.getEventId()).isNotBlank();
		assertThat(event.getSchemaVersion()).isEqualTo(1);
		assertThat(event.getSourceUserId()).isEqualTo(SOURCE_ID);
		assertThat(event.getTargetUserId()).isEqualTo(TARGET_ID);
		assertThat(event.getOccurredAt()).isEqualTo(NOW);
		assertThat(event.getStatus()).isEqualTo(UserMergedOutboxStatus.PENDING);
		assertThat(event.getAttemptCount()).isZero();
		assertThat(event.toString()).doesNotContain(SOURCE_ID, TARGET_ID);
	}

	@Test
	void rejectsSameOrNonCanonicalUserIds() {
		assertThatThrownBy(() -> UserMergedOutbox.create(SOURCE_ID, SOURCE_ID, NOW))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> UserMergedOutbox.create("not-a-uuid", TARGET_ID, NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
