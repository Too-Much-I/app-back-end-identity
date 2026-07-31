package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

class UserFactoryTests {

	private static final String AUDIO_POLICY_VERSION = "test-audio-policy-v1";
	private static final String GUEST_INSTALLATION_HASH = "A".repeat(43);
	private static final Instant GUEST_CREATED_AT = Instant.parse("2026-07-30T08:00:00Z");

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
	private final UserFactory userFactory = new UserFactory(
			new EmailNormalizer(),
			passwordEncoder,
			AUDIO_POLICY_VERSION
	);

	@Test
	void createsUserWithUuidIdentifier() {
		User user = createUser();

		assertThat(UUID.fromString(user.getUserId()).toString()).isEqualTo(user.getUserId());
	}

	@Test
	void createsActiveUser() {
		User user = createUser();

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getProvider()).isEqualTo(UserProvider.LOCAL);
	}

	@Test
	void createsActiveGuestWithoutEmailOrPasswordAndWithConsentMetadata() {
		User guest = userFactory.createGuest(GUEST_INSTALLATION_HASH, GUEST_CREATED_AT);

		assertThat(UUID.fromString(guest.getUserId()).toString()).isEqualTo(guest.getUserId());
		assertThat(guest.getProvider()).isEqualTo(UserProvider.GUEST);
		assertThat(guest.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(guest.getEmail()).isNull();
		assertThat(guest.getNormalizedEmail()).isNull();
		assertThat(guest.getPasswordHash()).isNull();
		assertThat(guest.getNickname()).isEqualTo(UserFactory.GUEST_NICKNAME);
		assertThat(guest.getGuestInstallationIdHash()).isEqualTo(GUEST_INSTALLATION_HASH);
		assertThat(guest.getAudioConsent().isAgreed()).isTrue();
		assertThat(guest.getAudioConsent().getPolicyVersion()).isEqualTo(AUDIO_POLICY_VERSION);
		assertThat(guest.getAudioConsent().getAgreedAt()).isEqualTo(GUEST_CREATED_AT);
		assertThat(guest.getAudioConsent().getWithdrawnAt()).isNull();
		assertThat(guest.getCreatedAt()).isEqualTo(GUEST_CREATED_AT);
		assertThat(guest.getUpdatedAt()).isEqualTo(GUEST_CREATED_AT);
	}

	@Test
	void createsMultipleGuestsWithNullLocalCredentialsAndUniqueUserIds() {
		User first = userFactory.createGuest("A".repeat(43), GUEST_CREATED_AT);
		User second = userFactory.createGuest("B".repeat(43), GUEST_CREATED_AT);

		assertThat(first.getUserId()).isNotEqualTo(second.getUserId());
		assertThat(first.getEmail()).isNull();
		assertThat(second.getEmail()).isNull();
		assertThat(first.getNormalizedEmail()).isNull();
		assertThat(second.getNormalizedEmail()).isNull();
		assertThat(first.getPasswordHash()).isNull();
		assertThat(second.getPasswordHash()).isNull();
	}

	@Test
	void mongoDocumentsOmitGuestLocalCredentialsExcludedByPartialEmailIndex() {
		MongoCustomConversions customConversions = MongoCustomConversions.create(
				adapter -> {
				}
		);
		MongoMappingContext mappingContext = new MongoMappingContext();
		mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
		mappingContext.afterPropertiesSet();
		MappingMongoConverter converter = new MappingMongoConverter(
				NoOpDbRefResolver.INSTANCE,
				mappingContext
		);
		converter.setCustomConversions(customConversions);
		converter.afterPropertiesSet();
		User first = userFactory.createGuest("A".repeat(43), GUEST_CREATED_AT);
		User second = userFactory.createGuest("B".repeat(43), GUEST_CREATED_AT);
		org.bson.Document firstDocument = new org.bson.Document();
		org.bson.Document secondDocument = new org.bson.Document();

		converter.write(first, firstDocument);
		converter.write(second, secondDocument);

		assertThat(firstDocument).doesNotContainKeys(
				"email",
				"normalizedEmail",
				"passwordHash"
		);
		assertThat(secondDocument).doesNotContainKeys(
				"email",
				"normalizedEmail",
				"passwordHash"
		);
		assertThat(firstDocument.getString("guestInstallationIdHash"))
				.isNotEqualTo(secondDocument.getString("guestInstallationIdHash"));
	}

	@Test
	void storesEncodedPasswordHashAndCanVerifyOriginalValue() {
		String rawCredential = "UnitOnly!234";

		User user = userFactory.create(
				"sample.user@example.test",
				rawCredential,
				"sample-nickname"
		);

		assertThat(user.getPasswordHash()).isNotEqualTo(rawCredential);
		assertThat(passwordEncoder.matches(rawCredential, user.getPasswordHash())).isTrue();
	}

	@Test
	void createsUserWithNormalizedEmail() {
		User user = userFactory.create(
				"  Sample.User@EXAMPLE.TEST  ",
				"UnitOnly!234",
				"sample-nickname"
		);

		assertThat(user.getEmail()).isEqualTo("Sample.User@EXAMPLE.TEST");
		assertThat(user.getNormalizedEmail()).isEqualTo("sample.user@example.test");
		assertThat(user.getGuestInstallationIdHash()).isNull();
	}

	@Test
	void createsUserWithTimestamps() {
		User user = createUser();

		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
	}

	@Test
	void createsAgreedAudioConsentWithConfiguredPolicyVersion() {
		User user = createUser();

		assertThat(user.getAudioConsent().isAgreed()).isTrue();
		assertThat(user.getAudioConsent().getPolicyVersion()).isEqualTo(AUDIO_POLICY_VERSION);
		assertThat(user.getAudioConsent().getAgreedAt()).isNotNull();
		assertThat(user.getAudioConsent().getWithdrawnAt()).isNull();
	}

	@Test
	void doesNotDeclarePlaintextPasswordField() {
		assertThat(Arrays.stream(User.class.getDeclaredFields()).map(Field::getName))
				.doesNotContain("password", "rawPassword");
	}

	@Test
	void declaresPartialUniqueIndexesForLocalEmailAndGuestInstallationHash()
			throws NoSuchFieldException {
		Document document = User.class.getAnnotation(Document.class);
		Field userId = User.class.getDeclaredField("userId");
		Indexed normalizedEmailIndex = User.class
				.getDeclaredField("normalizedEmail")
				.getAnnotation(Indexed.class);
		Indexed guestInstallationIndex = User.class
				.getDeclaredField("guestInstallationIdHash")
				.getAnnotation(Indexed.class);

		assertThat(document.collection()).isEqualTo("users");
		assertThat(userId.isAnnotationPresent(Id.class)).isTrue();
		assertThat(normalizedEmailIndex.unique()).isTrue();
		assertThat(normalizedEmailIndex.name())
				.isEqualTo("uk_users_normalized_email_present");
		assertThat(normalizedEmailIndex.partialFilter())
				.contains("normalizedEmail", "$type", "string");
		assertThat(guestInstallationIndex.unique()).isTrue();
		assertThat(guestInstallationIndex.name())
				.isEqualTo("uk_users_guest_installation_id_hash");
		assertThat(guestInstallationIndex.partialFilter())
				.contains("guestInstallationIdHash", "$type", "string");
	}

	@Test
	void storesOnlyInstallationHashFieldRatherThanRawInstallationId() {
		assertThat(Arrays.stream(User.class.getDeclaredFields()).map(Field::getName))
				.contains("guestInstallationIdHash")
				.doesNotContain("installationId", "guestInstallationId");
	}

	private User createUser() {
		return userFactory.create(
				"sample.user@example.test",
				"UnitOnly!234",
				"sample-nickname"
		);
	}
}
