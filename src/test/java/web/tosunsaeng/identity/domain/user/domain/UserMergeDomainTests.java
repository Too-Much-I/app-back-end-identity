package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

class UserMergeDomainTests {

	private static final Instant CREATED_AT = Instant.parse("2026-08-20T01:00:00Z");
	private static final Instant MERGED_AT = Instant.parse("2026-08-20T02:00:00Z");
	private static final String TARGET_ID = "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e";

	@Test
	void activeGuestBecomesCredentialFreeMergedTombstone() {
		User source = User.createGuest(
				"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
				"게스트",
				UserConsents.unconsented(),
				CREATED_AT
		);

		User merged = source.toMergedTombstone(TARGET_ID, MERGED_AT);

		assertThat(merged.getStatus()).isEqualTo(UserStatus.MERGED);
		assertThat(merged.getMergedIntoUserId()).isEqualTo(TARGET_ID);
		assertThat(merged.getMergedAt()).isEqualTo(MERGED_AT);
		assertThat(merged.getUpdatedAt()).isEqualTo(MERGED_AT);
		assertThat(merged.getGuestInstallationIdHash()).isNull();
		assertThat(merged.getEmail()).isNull();
		assertThat(merged.getPasswordHash()).isNull();
		assertThat(merged.getNickname()).isEqualTo(User.MERGED_NICKNAME);
		assertThat(merged.isGuest()).isTrue();
	}

	@Test
	void rejectsMemberAndSelfMerge() {
		User member = User.createFederatedMember(
				"회원",
				UserConsents.unconsented(),
				CREATED_AT
		);
		assertThatThrownBy(() -> member.toMergedTombstone(TARGET_ID, MERGED_AT))
				.isInstanceOf(IllegalStateException.class);

		User guest = User.createGuest(
				"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
				"게스트",
				UserConsents.unconsented(),
				CREATED_AT
		);
		assertThatThrownBy(() -> guest.toMergedTombstone(guest.getUserId(), MERGED_AT))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
