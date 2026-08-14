package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;

class PhoneIdentityTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant CREATED_AT = Instant.parse("2026-08-14T01:02:03Z");
	private static final String FINGERPRINT_A = "A".repeat(43);
	private static final String FINGERPRINT_B = "B".repeat(43);

	@Test
	void createsActiveIdentityAndAliasWithoutPhoneOrLastFourFields() {
		PhoneFingerprint fingerprint = new PhoneFingerprint("v1", FINGERPRINT_A);
		PhoneIdentity identity = PhoneIdentity.create(USER_ID, fingerprint, CREATED_AT);
		PhoneFingerprintAlias alias = PhoneFingerprintAlias.create(
				identity.getPhoneIdentityId(),
				USER_ID,
				fingerprint,
				CREATED_AT
		);

		assertThat(identity.getStatus()).isEqualTo(PhoneIdentityStatus.ACTIVE);
		assertThat(identity.getVerifiedAt()).isEqualTo(CREATED_AT);
		assertThat(identity.getReleasedAt()).isNull();
		assertThat(alias.getStatus()).isEqualTo(PhoneFingerprintAliasStatus.ACTIVE);
		assertThat(alias.matches(fingerprint)).isTrue();
		assertThat(identity.toString()).doesNotContain(FINGERPRINT_A).contains("[REDACTED]");
		assertThat(alias.toString()).doesNotContain(FINGERPRINT_A).contains("[REDACTED]");
		assertThat(PhoneIdentity.class.getDeclaredFields())
				.extracting(java.lang.reflect.Field::getName)
				.doesNotContain("phone", "phoneNumber", "phoneLast4", "last4");
	}

	@Test
	void rotatesCurrentFingerprintAndReleasesLifecycleMonotonically() {
		PhoneIdentity identity = PhoneIdentity.create(
				USER_ID,
				new PhoneFingerprint("v1", FINGERPRINT_A),
				CREATED_AT
		);
		Instant rotatedAt = CREATED_AT.plusSeconds(10);
		Instant releasedAt = rotatedAt.plusSeconds(10);

		assertThat(identity.rotateCurrentFingerprint(
				new PhoneFingerprint("v2", FINGERPRINT_B),
				rotatedAt
		)).isTrue();
		assertThat(identity.hasCurrentFingerprint(
				new PhoneFingerprint("v2", FINGERPRINT_B)
		)).isTrue();
		assertThat(identity.rotateCurrentFingerprint(
				new PhoneFingerprint("v2", FINGERPRINT_B),
				rotatedAt
		)).isFalse();
		assertThat(identity.release(releasedAt)).isTrue();
		assertThat(identity.release(releasedAt)).isFalse();
		assertThat(identity.getStatus()).isEqualTo(PhoneIdentityStatus.RELEASED);
		assertThat(identity.getReleasedAt()).isEqualTo(releasedAt);
		assertThatThrownBy(() -> identity.rotateCurrentFingerprint(
				new PhoneFingerprint("v3", FINGERPRINT_A),
				releasedAt
		)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsInvalidIdentifiersAndBackwardTimestampsWithoutSensitiveValues() {
		assertThatThrownBy(() -> PhoneIdentity.create(
				"not-a-uuid",
				new PhoneFingerprint("v1", FINGERPRINT_A),
				CREATED_AT
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining(FINGERPRINT_A);

		PhoneIdentity identity = PhoneIdentity.create(
				USER_ID,
				new PhoneFingerprint("v1", FINGERPRINT_A),
				CREATED_AT
		);
		assertThatThrownBy(() -> identity.release(CREATED_AT.minusSeconds(1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining(FINGERPRINT_A);
	}
}
