package web.tosunsaeng.identity.domain.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

class SocialIdentityTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant CREATED_AT = Instant.parse("2026-08-13T01:02:03Z");

	@Test
	void createsIdentityWithCanonicalUserReferenceAndServerGeneratedId() {
		SocialIdentity identity = SocialIdentity.create(
				USER_ID,
				SocialProvider.GOOGLE,
				"GoogleSubject_AbC123",
				CREATED_AT
		);

		assertThat(UUID.fromString(identity.getSocialIdentityId()).toString())
				.isEqualTo(identity.getSocialIdentityId());
		assertThat(identity.getUserId()).isEqualTo(USER_ID);
		assertThat(identity.getProvider()).isEqualTo(SocialProvider.GOOGLE);
		assertThat(identity.getProviderSubject()).isEqualTo("GoogleSubject_AbC123");
		assertThat(identity.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void supportsOnlyPlannedSocialProviderNamespaces() {
		assertThat(SocialProvider.values())
				.containsExactly(
						SocialProvider.GOOGLE,
						SocialProvider.KAKAO,
						SocialProvider.APPLE
				);
	}

	@Test
	void preservesProviderSubjectAsCaseSensitiveOpaqueValueWithoutTrimming() {
		String opaqueSubject = "  CaseSensitive:Subject/AbC  ";

		SocialIdentity identity = SocialIdentity.create(
				USER_ID,
				SocialProvider.KAKAO,
				opaqueSubject,
				CREATED_AT
		);

		assertThat(identity.getProviderSubject()).isEqualTo(opaqueSubject);
	}

	@Test
	void acceptsProviderSubjectAtMaximumLength() {
		String maximumLengthSubject = "s".repeat(255);

		SocialIdentity identity = SocialIdentity.create(
				USER_ID,
				SocialProvider.APPLE,
				maximumLengthSubject,
				CREATED_AT
		);

		assertThat(identity.getProviderSubject()).hasSize(255);
	}

	@Test
	void rejectsMissingBlankOrOversizedProviderSubject() {
		assertThatThrownBy(() -> SocialIdentity.create(
				USER_ID,
				SocialProvider.GOOGLE,
				null,
				CREATED_AT
		)).isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> SocialIdentity.create(
				USER_ID,
				SocialProvider.GOOGLE,
				"   ",
				CREATED_AT
		)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> SocialIdentity.create(
				USER_ID,
				SocialProvider.GOOGLE,
				"s".repeat(256),
				CREATED_AT
		)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsInvalidUserIdMissingProviderAndMissingCreatedAt() {
		assertThatThrownBy(() -> SocialIdentity.create(
				"not-a-uuid",
				SocialProvider.GOOGLE,
				"subject",
				CREATED_AT
		)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> SocialIdentity.create(
				USER_ID,
				null,
				"subject",
				CREATED_AT
		)).isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> SocialIdentity.create(
				USER_ID,
				SocialProvider.GOOGLE,
				"subject",
				null
		)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void declaresCollectionIdentifierAndRequiredIndexes() throws NoSuchFieldException {
		Document document = SocialIdentity.class.getAnnotation(Document.class);
		Field socialIdentityId = SocialIdentity.class.getDeclaredField("socialIdentityId");
		Indexed userIdIndex = SocialIdentity.class
				.getDeclaredField("userId")
				.getAnnotation(Indexed.class);
		CompoundIndex providerSubjectIndex = SocialIdentity.class
				.getAnnotation(CompoundIndex.class);

		assertThat(document.collection()).isEqualTo("social_identities");
		assertThat(socialIdentityId.isAnnotationPresent(Id.class)).isTrue();
		assertThat(providerSubjectIndex.name())
				.isEqualTo("uk_social_identities_provider_subject");
		assertThat(providerSubjectIndex.unique()).isTrue();
		assertThat(providerSubjectIndex.def())
				.isEqualTo("{ 'provider': 1, 'providerSubject': 1 }");
		assertThat(userIdIndex.name()).isEqualTo("ix_social_identities_user_id");
		assertThat(userIdIndex.unique()).isFalse();
	}

	@Test
	void storesOnlyMinimalIdentityFieldsWithoutDbRefOrEmailSnapshot() {
		assertThat(Arrays.stream(SocialIdentity.class.getDeclaredFields())
				.filter(field -> !Modifier.isStatic(field.getModifiers()))
				.map(Field::getName))
				.containsExactly(
						"socialIdentityId",
						"userId",
						"provider",
						"providerSubject",
						"createdAt"
				)
				.doesNotContain(
						"email",
						"updatedAt",
						"token",
						"claims",
						"user"
				);
		assertThat(Arrays.stream(SocialIdentity.class.getDeclaredFields())
				.anyMatch(field -> field.isAnnotationPresent(DBRef.class))).isFalse();
	}
}
