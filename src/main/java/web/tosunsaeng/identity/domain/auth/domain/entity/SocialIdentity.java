package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

@Document(collection = "social_identities")
@CompoundIndex(
		name = "uk_social_identities_provider_subject",
		def = "{ 'provider': 1, 'providerSubject': 1 }",
		unique = true
)
public class SocialIdentity {

	private static final int MAX_PROVIDER_SUBJECT_LENGTH = 255;

	@Id
	private String socialIdentityId;

	@Indexed(name = "ix_social_identities_user_id")
	private String userId;

	private SocialProvider provider;

	private String providerSubject;

	private Instant createdAt;

	private SocialIdentity() {
	}

	private SocialIdentity(
			String socialIdentityId,
			String userId,
			SocialProvider provider,
			String providerSubject,
			Instant createdAt
	) {
		this.socialIdentityId = requireUuid(socialIdentityId, "socialIdentityId");
		this.userId = requireUuid(userId, "userId");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.providerSubject = requireProviderSubject(providerSubject);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public static SocialIdentity create(
			String userId,
			SocialProvider provider,
			String providerSubject,
			Instant createdAt
	) {
		return new SocialIdentity(
				UUID.randomUUID().toString(),
				userId,
				provider,
				providerSubject,
				createdAt
		);
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equalsIgnoreCase(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(fieldName + " must be a UUID.");
		}
	}

	private static String requireProviderSubject(String providerSubject) {
		String requiredSubject = Objects.requireNonNull(
				providerSubject,
				"providerSubject must not be null"
		);
		if (requiredSubject.isBlank()) {
			throw new IllegalArgumentException("providerSubject must not be blank");
		}
		if (requiredSubject.length() > MAX_PROVIDER_SUBJECT_LENGTH) {
			throw new IllegalArgumentException("providerSubject must be at most 255 characters");
		}
		return requiredSubject;
	}

	public String getSocialIdentityId() {
		return socialIdentityId;
	}

	public String getUserId() {
		return userId;
	}

	public SocialProvider getProvider() {
		return provider;
	}

	public String getProviderSubject() {
		return providerSubject;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
