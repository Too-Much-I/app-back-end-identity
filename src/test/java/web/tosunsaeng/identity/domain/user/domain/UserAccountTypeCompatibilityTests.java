package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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

class UserAccountTypeCompatibilityTests {

	private static final Instant CREATED_AT = Instant.parse("2026-08-13T00:00:00Z");
	private static final String GUEST_INSTALLATION_HASH = "A".repeat(43);

	private final MappingMongoConverter converter = converter();

	@Test
	void accountTypeContainsOnlyGuestAndMember() {
		assertThat(UserAccountType.values()).containsExactly(
				UserAccountType.GUEST,
				UserAccountType.MEMBER
		);
	}

	@Test
	void newLocalAndGuestDocumentsDualWriteAccountTypeAndLegacyProvider() {
		Document localDocument = write(localUser());
		Document guestDocument = write(guestUser());

		assertThat(localDocument)
				.containsEntry("accountType", "MEMBER")
				.containsEntry("provider", "LOCAL");
		assertThat(guestDocument)
				.containsEntry("accountType", "GUEST")
				.containsEntry("provider", "GUEST");
	}

	@Test
	void storedAccountTypeTakesPriorityOverLegacyProvider() {
		Document document = write(guestUser());
		document.put("accountType", "MEMBER");

		User user = converter.read(User.class, document);

		assertThat(user.getAccountType()).isEqualTo(UserAccountType.MEMBER);
		assertThat(user.isMember()).isTrue();
		assertThat(user.isGuest()).isFalse();
		assertThat(user.getProvider()).isEqualTo(UserProvider.GUEST);
	}

	@Test
	void legacyGuestProviderFallsBackToGuestAccountType() {
		Document document = write(guestUser());
		document.remove("accountType");

		User user = converter.read(User.class, document);

		assertThat(user.getAccountType()).isEqualTo(UserAccountType.GUEST);
		assertThat(user.isGuest()).isTrue();
		assertThat(user.isMember()).isFalse();
	}

	@Test
	void legacyLocalProviderFallsBackToMemberAccountType() {
		Document document = write(localUser());
		document.remove("accountType");

		User user = converter.read(User.class, document);

		assertThat(user.getAccountType()).isEqualTo(UserAccountType.MEMBER);
		assertThat(user.isMember()).isTrue();
		assertThat(user.hasLocalCredential()).isTrue();
	}

	@Test
	void legacyMissingProviderFallsBackToMemberAccountType() {
		Document document = write(localUser());
		document.remove("accountType");
		document.remove("provider");

		User user = converter.read(User.class, document);

		assertThat(user.getAccountType()).isEqualTo(UserAccountType.MEMBER);
		assertThat(user.isMember()).isTrue();
		assertThat(user.getProvider()).isEqualTo(UserProvider.LOCAL);
	}

	@Test
	void memberAccountTypeDoesNotRequireLocalCredentialFields() {
		Document document = write(localUser());
		document.remove("email");
		document.remove("normalizedEmail");
		document.remove("passwordHash");
		document.put("accountType", "MEMBER");

		User user = converter.read(User.class, document);

		assertThat(user.getAccountType()).isEqualTo(UserAccountType.MEMBER);
		assertThat(user.isMember()).isTrue();
		assertThat(user.hasLocalCredential()).isFalse();
	}

	@Test
	void localFactoryStillRequiresLocalCredentialFields() {
		assertThatNullPointerException()
				.isThrownBy(() -> User.create(
						null,
						null,
						null,
						"로컬회원",
						consents(),
						CREATED_AT
				))
				.withMessage("email must not be null");
	}

	private User localUser() {
		return User.create(
				"compatibility.user@example.test",
				"compatibility.user@example.test",
				"encoded-password",
				"호환테스트",
				consents(),
				CREATED_AT
		);
	}

	private User guestUser() {
		return User.createGuest(
				GUEST_INSTALLATION_HASH,
				"게스트",
				consents(),
				CREATED_AT
		);
	}

	private UserConsents consents() {
		return UserConsents.consented("privacy-v1", "term-v1", CREATED_AT);
	}

	private Document write(User user) {
		Document document = new Document();
		converter.write(user, document);
		return document;
	}

	private static MappingMongoConverter converter() {
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
}
