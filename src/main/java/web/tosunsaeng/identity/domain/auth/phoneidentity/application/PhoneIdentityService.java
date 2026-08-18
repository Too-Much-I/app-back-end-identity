package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;

import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;

public final class PhoneIdentityService {

	private static final int MAX_CONCURRENCY_ATTEMPTS = 4;

	private final PhoneNumberNormalizer phoneNumberNormalizer;
	private final PhoneFingerprintHasher fingerprintHasher;
	private final PhoneIdentityTransactionService transactionService;
	private final PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher;
	private final Clock clock;

	public PhoneIdentityService(
			PhoneNumberNormalizer phoneNumberNormalizer,
			PhoneFingerprintHasher fingerprintHasher,
			PhoneIdentityTransactionService transactionService,
			Clock clock
	) {
		this.phoneNumberNormalizer = Objects.requireNonNull(
				phoneNumberNormalizer,
				"phoneNumberNormalizer must not be null"
		);
		this.fingerprintHasher = Objects.requireNonNull(
				fingerprintHasher,
				"fingerprintHasher must not be null"
		);
		this.transactionService = Objects.requireNonNull(
				transactionService,
				"transactionService must not be null"
		);
		this.eligibilityFingerprintHasher = null;
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	public PhoneIdentityService(
			PhoneNumberNormalizer phoneNumberNormalizer,
			PhoneFingerprintHasher fingerprintHasher,
			PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher,
			PhoneIdentityTransactionService transactionService,
			Clock clock
	) {
		this.phoneNumberNormalizer = Objects.requireNonNull(phoneNumberNormalizer);
		this.fingerprintHasher = Objects.requireNonNull(fingerprintHasher);
		this.eligibilityFingerprintHasher = eligibilityFingerprintHasher;
		this.transactionService = Objects.requireNonNull(transactionService);
		this.clock = Objects.requireNonNull(clock);
	}

	public PhoneIdentityLinkResult linkOrReplace(
			String userId,
			String verifiedPhoneNumber
	) {
		String requiredUserId = requireUuid(userId);
		String normalizedE164;
		try {
			normalizedE164 = phoneNumberNormalizer.normalize(verifiedPhoneNumber);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new AuthException(AuthErrorStatus.INVALID_PHONE_NUMBER);
		}
		PhoneFingerprintSet fingerprints = fingerprintHasher.fingerprint(normalizedE164);
		for (int attempt = 0; attempt < MAX_CONCURRENCY_ATTEMPTS; attempt++) {
			try {
				Instant verifiedAt = clock.instant();
				if (eligibilityFingerprintHasher == null) {
					return transactionService.linkOrReplace(requiredUserId, fingerprints, verifiedAt);
				}
				return transactionService.linkOrReplace(
						requiredUserId, fingerprints,
						eligibilityFingerprintHasher.consumerScopeId(),
						eligibilityFingerprintHasher.fingerprint(normalizedE164), verifiedAt);
			} catch (DuplicateKeyException | ConcurrencyFailureException exception) {
				// 재실행하면 승자의 alias를 조회해 멱등 성공 또는 PHONE_ALREADY_LINKED로 수렴한다.
			}
		}
		throw new AuthException(AuthErrorStatus.PHONE_IDENTITY_CONFLICT);
	}

	private static String requireUuid(String value) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equalsIgnoreCase(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException("userId must be a UUID.");
		}
	}
}
