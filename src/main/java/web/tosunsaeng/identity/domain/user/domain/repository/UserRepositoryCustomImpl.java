package web.tosunsaeng.identity.domain.user.domain.repository;

import java.time.Instant;
import java.util.Objects;

import com.mongodb.client.result.UpdateResult;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

public class UserRepositoryCustomImpl implements UserRepositoryCustom {

	private final MongoOperations mongoOperations;

	public UserRepositoryCustomImpl(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	@Override
	public boolean withdrawIfUnchanged(
			User withdrawnUser,
			UserStatus expectedStatus,
			Instant expectedUpdatedAt
	) {
		User requiredUser = Objects.requireNonNull(
				withdrawnUser,
				"withdrawnUser must not be null"
		);
		if (requiredUser.getStatus() != UserStatus.WITHDRAWN) {
			throw new IllegalArgumentException("User must be a withdrawn tombstone.");
		}

		Query query = Query.query(Criteria.where("_id")
				.is(requiredUser.getUserId())
				.and("status").is(Objects.requireNonNull(expectedStatus))
				.and("updatedAt").is(expectedUpdatedAt));
		Update update = new Update()
				.set("status", UserStatus.WITHDRAWN)
				.set("nickname", requiredUser.getNickname())
				.set("updatedAt", requiredUser.getUpdatedAt())
				.set("withdrawnAt", requiredUser.getWithdrawnAt())
				.unset("email")
				.unset("normalizedEmail")
				.unset("passwordHash")
				.unset("guestInstallationIdHash");

		UpdateResult result = mongoOperations.updateFirst(query, update, User.class);
		return result.getModifiedCount() == 1;
	}

	@Override
	public boolean updateConsentsIfActive(User user, Instant expectedUpdatedAt) {
		User requiredUser = Objects.requireNonNull(user, "user must not be null");
		Query query = Query.query(Criteria.where("_id")
				.is(requiredUser.getUserId())
				.and("status").is(UserStatus.ACTIVE)
				.and("updatedAt").is(expectedUpdatedAt));
		Update update = new Update()
				.set("consents", requiredUser.getConsents())
				.set("updatedAt", requiredUser.getUpdatedAt());

		UpdateResult result = mongoOperations.updateFirst(query, update, User.class);
		return result.getModifiedCount() == 1;
	}

	@Override
	public boolean promoteGuestIfUnchanged(User user, Instant expectedUpdatedAt) {
		User requiredUser = Objects.requireNonNull(user, "user must not be null");
		if (!requiredUser.isMember() || requiredUser.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalArgumentException("User must be an active promoted MEMBER.");
		}
		Criteria legacyOrExplicitGuest = new Criteria().orOperator(
				Criteria.where("accountType").is("GUEST"),
				new Criteria().andOperator(
						Criteria.where("accountType").exists(false),
						Criteria.where("provider").is("GUEST")
				)
		);
		Query query = Query.query(Criteria.where("_id")
				.is(requiredUser.getUserId())
				.and("status").is(UserStatus.ACTIVE)
				.and("updatedAt").is(expectedUpdatedAt)
				.andOperator(legacyOrExplicitGuest));
		Update update = new Update()
				.set("nickname", requiredUser.getNickname())
				.set("provider", requiredUser.getProvider())
				.set("accountType", requiredUser.getAccountType())
				.set("consents", requiredUser.getConsents())
				.set("updatedAt", requiredUser.getUpdatedAt())
				.unset("guestInstallationIdHash");
		UpdateResult result = mongoOperations.updateFirst(query, update, User.class);
		return result.getModifiedCount() == 1;
	}
}
