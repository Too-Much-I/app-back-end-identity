package web.tosunsaeng.identity.domain.user.domain.repository;

import java.time.Instant;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

public interface UserRepositoryCustom {

	boolean withdrawIfUnchanged(
			User withdrawnUser,
			UserStatus expectedStatus,
			Instant expectedUpdatedAt
	);

	boolean updateConsentsIfActive(User user, Instant expectedUpdatedAt);

	boolean promoteGuestIfUnchanged(User user, Instant expectedUpdatedAt);
}
