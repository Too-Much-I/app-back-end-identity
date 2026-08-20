package web.tosunsaeng.identity.domain.user.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

@Document(collection = "users")
public class User {

	public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";
	public static final String MERGED_NICKNAME = "통합된 사용자";

	@Id
	private String userId;

	private String email;

	@Indexed(
			name = "uk_users_normalized_email_present",
			unique = true,
			partialFilter = "{ 'normalizedEmail': { '$type': 'string' } }"
	)
	private String normalizedEmail;

	private String passwordHash;

	@Indexed(
			name = "uk_users_guest_installation_id_hash",
			unique = true,
			partialFilter = "{ 'guestInstallationIdHash': { '$type': 'string' } }"
	)
	private String guestInstallationIdHash;

	private String nickname;

	private UserProvider provider;

	private UserAccountType accountType;

	private UserConsents consents;

	private UserStatus status;

	private Instant createdAt;

	private Instant updatedAt;

	private Instant withdrawnAt;

	private String mergedIntoUserId;

	private Instant mergedAt;

	private User() {
	}

	private User(
			String userId,
			String email,
			String normalizedEmail,
			String passwordHash,
			String guestInstallationIdHash,
			String nickname,
			UserProvider provider,
			UserAccountType accountType,
			UserConsents consents,
			UserStatus status,
			Instant createdAt,
			Instant updatedAt,
			Instant withdrawnAt,
			String mergedIntoUserId,
			Instant mergedAt
	) {
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.email = email;
		this.normalizedEmail = normalizedEmail;
		this.passwordHash = passwordHash;
		this.guestInstallationIdHash = guestInstallationIdHash;
		this.nickname = Objects.requireNonNull(nickname, "nickname must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.accountType = Objects.requireNonNull(accountType, "accountType must not be null");
		this.consents = Objects.requireNonNull(consents, "consents must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.withdrawnAt = withdrawnAt;
		this.mergedIntoUserId = mergedIntoUserId;
		this.mergedAt = mergedAt;
		validateAccountFields();
	}

	public static User create(
			String email,
			String normalizedEmail,
			String passwordHash,
			String nickname,
			UserConsents consents,
			Instant createdAt
	) {
		String requiredEmail = Objects.requireNonNull(email, "email must not be null");
		String requiredNormalizedEmail = Objects.requireNonNull(
				normalizedEmail,
				"normalizedEmail must not be null"
		);
		String requiredPasswordHash = Objects.requireNonNull(
				passwordHash,
				"passwordHash must not be null"
		);
		return new User(
				UUID.randomUUID().toString(),
				requiredEmail,
				requiredNormalizedEmail,
				requiredPasswordHash,
				null,
				nickname,
				UserProvider.LOCAL,
				UserAccountType.MEMBER,
				consents,
				UserStatus.ACTIVE,
				createdAt,
				createdAt,
				null,
				null,
				null
		);
	}

	public static User createGuest(
			String guestInstallationIdHash,
			String nickname,
			UserConsents consents,
			Instant createdAt
	) {
		return new User(
				UUID.randomUUID().toString(),
				null,
				null,
				null,
				guestInstallationIdHash,
				nickname,
				UserProvider.GUEST,
				UserAccountType.GUEST,
				consents,
				UserStatus.ACTIVE,
				createdAt,
				createdAt,
				null,
				null,
				null
		);
	}

	public static User createFederatedMember(
			String nickname,
			UserConsents consents,
			Instant createdAt
	) {
		return new User(
				UUID.randomUUID().toString(),
				null,
				null,
				null,
				null,
				nickname,
				UserProvider.FEDERATED,
				UserAccountType.MEMBER,
				consents,
				UserStatus.ACTIVE,
				createdAt,
				createdAt,
				null,
				null,
				null
		);
	}

	private void validateAccountFields() {
		if (status == UserStatus.MERGED) {
			if (email != null
					|| normalizedEmail != null
					|| passwordHash != null
					|| guestInstallationIdHash != null) {
				throw new IllegalArgumentException(
						"Merged user credentials must be absent."
				);
			}
			requireUuid(mergedIntoUserId, "mergedIntoUserId");
			Objects.requireNonNull(mergedAt, "mergedAt must not be null");
			if (userId.equals(mergedIntoUserId)) {
				throw new IllegalArgumentException("Merged user must target another user.");
			}
			return;
		}
		if (mergedIntoUserId != null || mergedAt != null) {
			throw new IllegalArgumentException("Only a merged user can have merge metadata.");
		}
		if (status == UserStatus.WITHDRAWN) {
			if (email != null
					|| normalizedEmail != null
					|| passwordHash != null
					|| guestInstallationIdHash != null) {
				throw new IllegalArgumentException(
						"Withdrawn user credentials must be absent."
				);
			}
			Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");
			return;
		}
		if (isGuest()) {
			if (email != null || normalizedEmail != null || passwordHash != null) {
				throw new IllegalArgumentException("Guest credentials must be absent.");
			}
			requireGuestInstallationIdHash(guestInstallationIdHash);
			return;
		}
		boolean anyLocalCredentialField = email != null
				|| normalizedEmail != null
				|| passwordHash != null;
		if (anyLocalCredentialField && !hasLocalCredential()) {
			throw new IllegalArgumentException(
					"Local credential fields must either all be present or all be absent."
			);
		}
	}

	public User toWithdrawnTombstone(Instant withdrawalTime) {
		if (status == UserStatus.WITHDRAWN) {
			return this;
		}
		Instant requiredWithdrawalTime = Objects.requireNonNull(
				withdrawalTime,
				"withdrawalTime must not be null"
		);
		return new User(
				userId,
				null,
				null,
				null,
				null,
				WITHDRAWN_NICKNAME,
				getProvider(),
				getAccountType(),
				getConsents(),
				UserStatus.WITHDRAWN,
				createdAt,
				requiredWithdrawalTime,
				requiredWithdrawalTime,
				null,
				null
		);
	}

	public User toMergedTombstone(String targetUserId, Instant mergeTime) {
		if (status != UserStatus.ACTIVE || !isGuest()) {
			throw new IllegalStateException("Only an active Guest can be merged.");
		}
		String requiredTargetUserId = requireUuid(targetUserId, "targetUserId");
		Instant requiredMergeTime = Objects.requireNonNull(
				mergeTime,
				"mergeTime must not be null"
		);
		return new User(
				userId,
				null,
				null,
				null,
				null,
				MERGED_NICKNAME,
				getProvider(),
				getAccountType(),
				getConsents(),
				UserStatus.MERGED,
				createdAt,
				requiredMergeTime,
				null,
				requiredTargetUserId,
				requiredMergeTime
		);
	}

	public void promoteGuestToFederatedMember(
			String memberNickname,
			String privacyConsentVersion,
			String termConsentVersion,
			Instant promotedAt
	) {
		if (status != UserStatus.ACTIVE || !isGuest()) {
			throw new IllegalStateException("Only an active Guest can be promoted.");
		}
		String requiredNickname = Objects.requireNonNull(
				memberNickname,
				"memberNickname must not be null"
		).trim();
		if (requiredNickname.isEmpty()) {
			throw new IllegalArgumentException("memberNickname must not be blank");
		}
		Instant requiredPromotedAt = Objects.requireNonNull(
				promotedAt,
				"promotedAt must not be null"
		);
		this.nickname = requiredNickname;
		this.provider = UserProvider.FEDERATED;
		this.accountType = UserAccountType.MEMBER;
		this.guestInstallationIdHash = null;
		this.consents = getConsents().renewRequiredConsents(
				privacyConsentVersion,
				termConsentVersion,
				requiredPromotedAt
		);
		this.updatedAt = requiredPromotedAt;
		validateAccountFields();
	}

	private static String requireGuestInstallationIdHash(String hash) {
		String requiredHash = Objects.requireNonNull(
				hash,
				"guestInstallationIdHash must not be null"
		);
		if (!requiredHash.matches("[A-Za-z0-9_-]{43}")) {
			throw new IllegalArgumentException("Guest installation hash has an invalid format.");
		}
		return requiredHash;
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equals(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(
					fieldName + " must be a lowercase canonical UUID."
			);
		}
	}

	public String getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getNormalizedEmail() {
		return normalizedEmail;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getGuestInstallationIdHash() {
		return guestInstallationIdHash;
	}

	public String getNickname() {
		return nickname;
	}

	public UserProvider getProvider() {
		return provider == null ? UserProvider.LOCAL : provider;
	}

	public UserAccountType getAccountType() {
		if (accountType != null) {
			return accountType;
		}
		return getProvider() == UserProvider.GUEST
				? UserAccountType.GUEST
				: UserAccountType.MEMBER;
	}

	public boolean isGuest() {
		return getAccountType() == UserAccountType.GUEST;
	}

	public boolean isMember() {
		return getAccountType() == UserAccountType.MEMBER;
	}

	public boolean hasLocalCredential() {
		return email != null && normalizedEmail != null && passwordHash != null;
	}

	public UserConsents getConsents() {
		return consents == null ? UserConsents.unconsented() : consents;
	}

	public boolean updateConsents(
			String privacyConsentVersion,
			String termConsentVersion,
			boolean qualityReviewConsented,
			String qualityReviewConsentVersion,
			Instant consentedAt
	) {
		Instant requiredConsentedAt = Objects.requireNonNull(
				consentedAt,
				"consentedAt must not be null"
		);
		UserConsents currentConsents = getConsents();
		UserConsents updatedConsents = currentConsents.renew(
				privacyConsentVersion,
				termConsentVersion,
				qualityReviewConsented,
				qualityReviewConsentVersion,
				requiredConsentedAt
		);
		if (updatedConsents == currentConsents) {
			return false;
		}
		this.consents = updatedConsents;
		this.updatedAt = requiredConsentedAt;
		return true;
	}

	public UserStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getWithdrawnAt() {
		return withdrawnAt;
	}

	public String getMergedIntoUserId() {
		return mergedIntoUserId;
	}

	public Instant getMergedAt() {
		return mergedAt;
	}
}
