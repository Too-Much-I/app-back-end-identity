package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

class UserWithdrawalDomainTests {

	private static final Instant CREATED_AT = Instant.parse("2026-08-01T00:00:00Z");
	private static final Instant WITHDRAWN_AT = Instant.parse("2026-08-07T01:23:45Z");

	@Test
	void localTombstoneRemovesCredentialsAndPreservesIdentityProviderAndConsents() {
		User local = localUser();

		User tombstone = local.toWithdrawnTombstone(WITHDRAWN_AT);

		assertThat(tombstone.getUserId()).isEqualTo(local.getUserId());
		assertThat(tombstone.getAccountType()).isEqualTo(UserAccountType.MEMBER);
		assertThat(tombstone.getProvider()).isEqualTo(UserProvider.LOCAL);
		assertThat(tombstone.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
		assertThat(tombstone.getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(tombstone.getUpdatedAt()).isEqualTo(WITHDRAWN_AT);
		assertThat(tombstone.getWithdrawnAt()).isEqualTo(WITHDRAWN_AT);
		assertThat(tombstone.getEmail()).isNull();
		assertThat(tombstone.getNormalizedEmail()).isNull();
		assertThat(tombstone.getPasswordHash()).isNull();
		assertThat(tombstone.hasLocalCredential()).isFalse();
		assertThat(tombstone.getGuestInstallationIdHash()).isNull();
		assertThat(tombstone.getNickname()).isEqualTo(User.WITHDRAWN_NICKNAME);
		assertThat(tombstone.getConsents()).isSameAs(local.getConsents());
		assertThat(local.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void guestTombstoneReleasesInstallationHashWithoutChangingOriginalUser() {
		User guest = guestUser();

		User tombstone = guest.toWithdrawnTombstone(WITHDRAWN_AT);

		assertThat(tombstone.getAccountType()).isEqualTo(UserAccountType.GUEST);
		assertThat(tombstone.getProvider()).isEqualTo(UserProvider.GUEST);
		assertThat(tombstone.getGuestInstallationIdHash()).isNull();
		assertThat(tombstone.getEmail()).isNull();
		assertThat(tombstone.getPasswordHash()).isNull();
		assertThat(guest.getGuestInstallationIdHash()).isEqualTo("A".repeat(43));
		assertThat(guest.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void withdrawnMongoDocumentOmitsAllCredentialAndUniqueIndexFields() {
		MappingMongoConverter converter = converter();
		Document document = new Document();

		converter.write(localUser().toWithdrawnTombstone(WITHDRAWN_AT), document);

		assertThat(document).doesNotContainKeys(
				"email",
				"normalizedEmail",
				"passwordHash",
				"guestInstallationIdHash"
		);
		assertThat(document.getString("status")).isEqualTo("WITHDRAWN");
		assertThat(document.getString("accountType")).isEqualTo("MEMBER");
		assertThat(document.get("withdrawnAt")).isNotNull();
		assertThat(document.get("consents")).isNotNull();
	}

	private MappingMongoConverter converter() {
		MongoCustomConversions conversions = MongoCustomConversions.create(adapter -> {
		});
		MongoMappingContext context = new MongoMappingContext();
		context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
		context.afterPropertiesSet();
		MappingMongoConverter converter = new MappingMongoConverter(
				NoOpDbRefResolver.INSTANCE,
				context
		);
		converter.setCustomConversions(conversions);
		converter.afterPropertiesSet();
		return converter;
	}

	private User localUser() {
		return User.create(
				"domain.user@example.test",
				"domain.user@example.test",
				"encoded-password",
				"도메인테스트",
				consents(),
				CREATED_AT
		);
	}

	private User guestUser() {
		return User.createGuest(
				"A".repeat(43),
				"게스트",
				consents(),
				CREATED_AT
		);
	}

	private UserConsents consents() {
		return UserConsents.consented("privacy-v1", "term-v1", CREATED_AT);
	}
}
