package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;

public final class UserWithdrawnBackfillService {

	public record Result(int selected, int missing, int created, int concurrentConflicts) {
	}

	private final MongoOperations mongoOperations;
	private final UserWithdrawnOutboxRepository repository;

	public UserWithdrawnBackfillService(
			MongoOperations mongoOperations,
			UserWithdrawnOutboxRepository repository
	) {
		this.mongoOperations = Objects.requireNonNull(mongoOperations);
		this.repository = Objects.requireNonNull(repository);
	}

	public Result run(
			Instant lowerBoundInclusive,
			Instant upperBoundExclusive,
			int batchLimit,
			boolean dryRun
	) {
		Instant lower = Objects.requireNonNull(lowerBoundInclusive);
		Instant upper = Objects.requireNonNull(upperBoundExclusive);
		if (!upper.isAfter(lower)) {
			throw new IllegalArgumentException("backfill upper bound must be after lower bound");
		}
		if (batchLimit < 1 || batchLimit > 100) {
			throw new IllegalArgumentException("backfill batch limit must be between 1 and 100");
		}
		Query query = Query.query(new Criteria().andOperator(
				Criteria.where("status").is(UserStatus.WITHDRAWN),
				Criteria.where("withdrawnAt").gte(lower).lt(upper)
		)).with(Sort.by(Sort.Direction.ASC, "withdrawnAt", "_id")).limit(batchLimit);
		List<User> users = mongoOperations.find(query, User.class);
		int missing = 0;
		int created = 0;
		int conflicts = 0;
		for (User user : users) {
			if (repository.existsByUserId(user.getUserId())) {
				continue;
			}
			missing++;
			if (dryRun) {
				continue;
			}
			try {
				repository.save(UserWithdrawnOutbox.createBackfill(
						user.getUserId(),
						user.getWithdrawnAt()
				));
				created++;
			} catch (DuplicateKeyException exception) {
				conflicts++;
			}
		}
		return new Result(users.size(), missing, created, conflicts);
	}
}
