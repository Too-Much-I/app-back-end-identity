package web.tosunsaeng.identity.domain.user.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

public final class UserWithdrawalIdentityReleaseWorker {

	private static final Logger log = LoggerFactory.getLogger(
			UserWithdrawalIdentityReleaseWorker.class
	);

	private final UserWithdrawalLifecycleRepository lifecycleRepository;
	private final UserWithdrawalIdentityReleaseTransactionService transactionService;
	private final MeterRegistry meterRegistry;
	private final Clock clock;

	public UserWithdrawalIdentityReleaseWorker(
			UserWithdrawalLifecycleRepository lifecycleRepository,
			UserWithdrawalIdentityReleaseTransactionService transactionService,
			MeterRegistry meterRegistry,
			Clock clock
	) {
		this.lifecycleRepository = Objects.requireNonNull(lifecycleRepository);
		this.transactionService = Objects.requireNonNull(transactionService);
		this.meterRegistry = Objects.requireNonNull(meterRegistry);
		this.clock = Objects.requireNonNull(clock);
	}

	public IdentityReleaseOutcome processNext() {
		Optional<UserWithdrawalLifecycle> selected = lifecycleRepository
				.findFirstByStatusOrderByExternalDeletedAtAscWithdrawalIdAsc(
						UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
				);
		if (selected.isEmpty()) {
			return record(IdentityReleaseOutcome.NONE);
		}
		try {
			return record(transactionService.release(selected.orElseThrow(), clock.instant()));
		} catch (OptimisticLockingFailureException exception) {
			return record(IdentityReleaseOutcome.CONCURRENT_CHANGE);
		} catch (TransientDataAccessException exception) {
			return record(IdentityReleaseOutcome.TRANSIENT_FAILURE);
		} catch (web.tosunsaeng.identity.domain.auth.common.exception.AuthException exception) {
			if (exception.getErrorCode() == web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING) {
				return record(IdentityReleaseOutcome.DEPENDENCY_PENDING);
			}
			throw exception;
		}
	}

	private IdentityReleaseOutcome record(IdentityReleaseOutcome outcome) {
		meterRegistry.counter(
				"identity.user.withdrawal.identity_release",
				"outcome", outcome.name()
		).increment();
		if (outcome != IdentityReleaseOutcome.NONE) {
			log.atInfo()
					.addKeyValue("event", "user.withdrawal.identity_release")
					.addKeyValue("outcome", outcome.name())
					.log("Withdrawal identity release outcome");
		}
		return outcome;
	}
}
