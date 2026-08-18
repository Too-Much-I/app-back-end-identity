package web.tosunsaeng.identity.domain.user.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

class UserRepositoryCustomImplTests {

	private static final Instant CREATED_AT = Instant.parse("2026-08-01T00:00:00Z");
	private static final Instant WITHDRAWN_AT = Instant.parse("2026-08-07T01:23:45Z");

	@Test
	void withdrawalUsesStatusAndUpdatedAtCompareAndSetAndUnsetsIdentifiers() {
		MongoOperations mongoOperations = mock(MongoOperations.class);
		UserRepositoryCustomImpl repository = new UserRepositoryCustomImpl(mongoOperations);
		User active = localUser();
		User tombstone = active.toWithdrawnTombstone(WITHDRAWN_AT);
		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
		when(mongoOperations.updateFirst(
				queryCaptor.capture(),
				updateCaptor.capture(),
				eq(User.class)
		)).thenReturn(UpdateResult.acknowledged(1, 1L, null));

		boolean updated = repository.withdrawIfUnchanged(
				tombstone,
				UserStatus.ACTIVE,
				CREATED_AT
		);

		assertThat(updated).isTrue();
		Document query = queryCaptor.getValue().getQueryObject();
		assertThat(query.getString("_id")).isEqualTo(active.getUserId());
		assertThat(query.get("status")).isEqualTo(UserStatus.ACTIVE);
		assertThat(query.get("updatedAt")).isEqualTo(CREATED_AT);
		Document update = updateCaptor.getValue().getUpdateObject();
		Document set = update.get("$set", Document.class);
		Document unset = update.get("$unset", Document.class);
		assertThat(set.get("status")).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(set.getString("nickname")).isEqualTo(User.WITHDRAWN_NICKNAME);
		assertThat(set.get("updatedAt")).isEqualTo(WITHDRAWN_AT);
		assertThat(set.get("withdrawnAt")).isEqualTo(WITHDRAWN_AT);
		assertThat(unset.keySet()).containsExactlyInAnyOrder(
				"email",
				"normalizedEmail",
				"passwordHash",
				"guestInstallationIdHash"
		);
	}

	@Test
	void consentUpdateIsPartialAndCannotMatchWithdrawnUser() {
		MongoOperations mongoOperations = mock(MongoOperations.class);
		UserRepositoryCustomImpl repository = new UserRepositoryCustomImpl(mongoOperations);
		User user = localUser();
		Instant previousUpdatedAt = user.getUpdatedAt();
		user.updateConsents(
				"privacy-v2",
				"term-v2",
				false,
				"quality-review-v1",
				WITHDRAWN_AT
		);
		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
		when(mongoOperations.updateFirst(
				queryCaptor.capture(),
				updateCaptor.capture(),
				eq(User.class)
		)).thenReturn(UpdateResult.acknowledged(1, 1L, null));

		assertThat(repository.updateConsentsIfActive(user, previousUpdatedAt)).isTrue();

		Document query = queryCaptor.getValue().getQueryObject();
		assertThat(query.get("status")).isEqualTo(UserStatus.ACTIVE);
		assertThat(query.get("updatedAt")).isEqualTo(previousUpdatedAt);
		Document set = updateCaptor.getValue().getUpdateObject().get("$set", Document.class);
		assertThat(set.keySet()).containsExactlyInAnyOrder("consents", "updatedAt");
		assertThat(set).doesNotContainKeys(
				"status",
				"email",
				"normalizedEmail",
				"passwordHash",
				"guestInstallationIdHash"
		);
		UserConsents storedConsents = (UserConsents) set.get("consents");
		assertThat(storedConsents.isQualityReviewConsented()).isFalse();
		assertThat(storedConsents.getQualityReviewConsentVersion()).isNull();
		assertThat(storedConsents.getQualityReviewConsentedAt()).isNull();
		verify(mongoOperations).updateFirst(
				any(Query.class),
				any(Update.class),
				eq(User.class)
		);
	}

	@Test
	void guestPromotionUsesGuestAndUpdatedAtCompareAndSetAndRemovesGuestCredential() {
		MongoOperations mongoOperations = mock(MongoOperations.class);
		UserRepositoryCustomImpl repository = new UserRepositoryCustomImpl(mongoOperations);
		User guest = User.createGuest(
				"A".repeat(43),
				"게스트",
				UserConsents.consented("privacy-v1", "term-v1", CREATED_AT),
				CREATED_AT
		);
		String userId = guest.getUserId();
		guest.promoteGuestToFederatedMember(
				"승격회원",
				"privacy-v1",
				"term-v1",
				WITHDRAWN_AT
		);
		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
		when(mongoOperations.updateFirst(
				queryCaptor.capture(),
				updateCaptor.capture(),
				eq(User.class)
		)).thenReturn(UpdateResult.acknowledged(1, 1L, null));

		assertThat(repository.promoteGuestIfUnchanged(guest, CREATED_AT)).isTrue();

		Document query = queryCaptor.getValue().getQueryObject();
		assertThat(query.getString("_id")).isEqualTo(userId);
		assertThat(query.get("status")).isEqualTo(UserStatus.ACTIVE);
		assertThat(query.get("updatedAt")).isEqualTo(CREATED_AT);
		assertThat(query).containsKey("$and");
		assertThat(query.get("$and").toString()).contains(
				"accountType",
				"GUEST",
				"provider"
		);
		Document update = updateCaptor.getValue().getUpdateObject();
		Document set = update.get("$set", Document.class);
		Document unset = update.get("$unset", Document.class);
		assertThat(set.getString("nickname")).isEqualTo("승격회원");
		assertThat(set.get("accountType").toString()).isEqualTo("MEMBER");
		assertThat(set.get("provider").toString()).isEqualTo("FEDERATED");
		assertThat(set.get("updatedAt")).isEqualTo(WITHDRAWN_AT);
		assertThat(unset).containsKey("guestInstallationIdHash");
	}

	private User localUser() {
		return User.create(
				"repository.user@example.test",
				"repository.user@example.test",
				"encoded-password",
				"저장소테스트",
				UserConsents.consented(
						"privacy-v1",
						"term-v1",
						true,
						"quality-review-v1",
						CREATED_AT
				),
				CREATED_AT
		);
	}
}
