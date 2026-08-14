package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKey;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKeyRegistry;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;

class PhoneIdentityServiceTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String IDENTITY_ID = "85a8c19e-5ab4-4945-bd1e-3a20ac269d9b";
	private static final Instant NOW = Instant.parse("2026-08-14T01:02:03Z");

	@Test
	void normalizesBeforeFingerprintingAndDelegatesAtApplicationTime() {
		PhoneIdentityTransactionService transaction = mock(PhoneIdentityTransactionService.class);
		PhoneIdentityLinkResult expected = new PhoneIdentityLinkResult(
				IDENTITY_ID,
				USER_ID,
				PhoneIdentityLinkOutcome.CREATED
		);
		when(transaction.linkOrReplace(eq(USER_ID), any(PhoneFingerprintSet.class), eq(NOW)))
				.thenReturn(expected);

		PhoneIdentityLinkResult result = service(transaction).linkOrReplace(
				USER_ID,
				"+1 (415) 555-2671"
		);

		assertThat(result).isEqualTo(expected);
		verify(transaction).linkOrReplace(eq(USER_ID), any(PhoneFingerprintSet.class), eq(NOW));
	}

	@Test
	void invalidPhoneMapsToStableErrorWithoutRepositoryCallOrInputLeak() {
		PhoneIdentityTransactionService transaction = mock(PhoneIdentityTransactionService.class);
		String invalid = "+1 invalid phone";

		assertThatThrownBy(() -> service(transaction).linkOrReplace(USER_ID, invalid))
				.isInstanceOfSatisfying(AuthException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(AuthErrorStatus.INVALID_PHONE_NUMBER);
					assertThat(exception).hasMessageNotContaining(invalid);
				});
		verify(transaction, never()).linkOrReplace(any(), any(), any());
	}

	@Test
	void concurrentDuplicateRetriesAndConvergesToWinnerResult() {
		PhoneIdentityTransactionService transaction = mock(PhoneIdentityTransactionService.class);
		PhoneIdentityLinkResult winner = new PhoneIdentityLinkResult(
				IDENTITY_ID,
				USER_ID,
				PhoneIdentityLinkOutcome.IDEMPOTENT
		);
		when(transaction.linkOrReplace(eq(USER_ID), any(PhoneFingerprintSet.class), eq(NOW)))
				.thenThrow(new DuplicateKeyException("concurrent claim"))
				.thenReturn(winner);

		assertThat(service(transaction).linkOrReplace(USER_ID, "+14155552671"))
				.isEqualTo(winner);
		verify(transaction, org.mockito.Mockito.times(2))
				.linkOrReplace(eq(USER_ID), any(PhoneFingerprintSet.class), eq(NOW));
	}

	@Test
	void unresolvedConcurrencyFailsWithStableConflict() {
		PhoneIdentityTransactionService transaction = mock(PhoneIdentityTransactionService.class);
		when(transaction.linkOrReplace(eq(USER_ID), any(PhoneFingerprintSet.class), eq(NOW)))
				.thenThrow(new DuplicateKeyException("concurrent claim"));

		assertThatThrownBy(() -> service(transaction).linkOrReplace(USER_ID, "+14155552671"))
				.isInstanceOfSatisfying(AuthException.class, exception -> assertThat(
						exception.getErrorCode()
				).isEqualTo(AuthErrorStatus.PHONE_IDENTITY_CONFLICT));
		verify(transaction, org.mockito.Mockito.times(4))
				.linkOrReplace(eq(USER_ID), any(PhoneFingerprintSet.class), eq(NOW));
	}

	private PhoneIdentityService service(PhoneIdentityTransactionService transaction) {
		byte[] material = new byte[32];
		Arrays.fill(material, (byte) 7);
		PhoneFingerprintKey key = PhoneFingerprintKey.fromBase64(
				"v1",
				PhoneFingerprintKeyStatus.ACTIVE_WRITE,
				Base64.getEncoder().encodeToString(material)
		);
		return new PhoneIdentityService(
				new PhoneNumberNormalizer(),
				new PhoneFingerprintHasher(new PhoneFingerprintKeyRegistry(List.of(key))),
				transaction,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}
}
