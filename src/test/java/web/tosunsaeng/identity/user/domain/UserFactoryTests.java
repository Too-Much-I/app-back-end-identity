package web.tosunsaeng.identity.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserFactoryTests {

	private static final String AUDIO_POLICY_VERSION = "test-audio-policy-v1";

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
	void declaresMongoDocumentIdAndUniqueNormalizedEmailIndex() throws NoSuchFieldException {
		Document document = User.class.getAnnotation(Document.class);
		Field userId = User.class.getDeclaredField("userId");
		Indexed normalizedEmailIndex = User.class
				.getDeclaredField("normalizedEmail")
				.getAnnotation(Indexed.class);

		assertThat(document.collection()).isEqualTo("users");
		assertThat(userId.isAnnotationPresent(Id.class)).isTrue();
		assertThat(normalizedEmailIndex.unique()).isTrue();
		assertThat(normalizedEmailIndex.name()).isEqualTo("uk_users_normalized_email");
	}

	private User createUser() {
		return userFactory.create(
				"sample.user@example.test",
				"UnitOnly!234",
				"sample-nickname"
		);
	}
}
