package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;

class UserWithdrawnBackfillServiceTests {

	private static final Instant LOWER = Instant.parse("2026-08-28T00:00:00Z");
	private static final Instant UPPER = Instant.parse("2026-08-28T01:00:00Z");

	@Test
	void dryRunReportsMissingWithoutMutation() {
		MongoOperations operations = mock(MongoOperations.class);
		UserWithdrawnOutboxRepository repository = mock(UserWithdrawnOutboxRepository.class);
		User withdrawn = withdrawnUser(LOWER.plusSeconds(30));
		when(operations.find(any(Query.class), eq(User.class))).thenReturn(List.of(withdrawn));
		when(repository.existsByUserId(withdrawn.getUserId())).thenReturn(false);

		UserWithdrawnBackfillService.Result result = service(operations, repository)
				.run(LOWER, UPPER, 20, true);

		assertThat(result).isEqualTo(new UserWithdrawnBackfillService.Result(1, 1, 0, 0));
		verify(repository, never()).save(any());
	}

	@Test
	void liveRunCreatesOnlyMissingDeterministicOutbox() {
		MongoOperations operations = mock(MongoOperations.class);
		UserWithdrawnOutboxRepository repository = mock(UserWithdrawnOutboxRepository.class);
		User existing = withdrawnUser(LOWER.plusSeconds(10));
		User missing = withdrawnUser(LOWER.plusSeconds(20));
		when(operations.find(any(Query.class), eq(User.class)))
				.thenReturn(List.of(existing, missing));
		when(repository.existsByUserId(existing.getUserId())).thenReturn(true);
		when(repository.existsByUserId(missing.getUserId())).thenReturn(false);

		UserWithdrawnBackfillService.Result result = service(operations, repository)
				.run(LOWER, UPPER, 20, false);

		assertThat(result).isEqualTo(new UserWithdrawnBackfillService.Result(2, 1, 1, 0));
		verify(repository).save(any(UserWithdrawnOutbox.class));
	}

	private UserWithdrawnBackfillService service(
			MongoOperations operations,
			UserWithdrawnOutboxRepository repository
	) {
		return new UserWithdrawnBackfillService(operations, repository);
	}

	private User withdrawnUser(Instant withdrawnAt) {
		return User.createFederatedMember(
				"회원",
				UserConsents.consented("privacy-v1", "term-v1", LOWER.minusSeconds(60)),
				LOWER.minusSeconds(60)
		).toWithdrawnTombstone(withdrawnAt);
	}
}
