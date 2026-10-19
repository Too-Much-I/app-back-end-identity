package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnOutboxStatus;

class UserWithdrawnOutboxTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant WITHDRAWN_AT = Instant.parse("2026-08-28T01:02:03Z");

	@Test
	void createsPendingVersionOneEventWithoutExposingIdentifiersInString() {
		UserWithdrawnOutbox event = UserWithdrawnOutbox.create(USER_ID, WITHDRAWN_AT);

		assertThat(event.getSchemaVersion()).isEqualTo(1);
		assertThat(event.getUserId()).isEqualTo(USER_ID);
		assertThat(event.getWithdrawnAt()).isEqualTo(WITHDRAWN_AT);
		assertThat(event.getStatus()).isEqualTo(UserWithdrawnOutboxStatus.PENDING);
		assertThat(event.getAttemptCount()).isZero();
		assertThat(event.getNextAttemptAt()).isEqualTo(WITHDRAWN_AT);
		assertThat(event.toString()).doesNotContain(USER_ID, event.getEventId());
	}

	@Test
	void backfillEventIdIsDeterministicAndInputsMustBeCanonical() {
		UserWithdrawnOutbox first = UserWithdrawnOutbox.createBackfill(USER_ID, WITHDRAWN_AT);
		UserWithdrawnOutbox second = UserWithdrawnOutbox.createBackfill(USER_ID, WITHDRAWN_AT);

		assertThat(first.getEventId()).isEqualTo(second.getEventId());
		assertThatThrownBy(() -> UserWithdrawnOutbox.create(
				USER_ID.toUpperCase(),
				WITHDRAWN_AT
		)).isInstanceOf(IllegalArgumentException.class);
	}
}
