package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.util.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.federation.repository.*;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.*;
import web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.*;

public final class RecoveryAccountResolver {
	private final PhoneFingerprintHasher hasher;
	private final PhoneFingerprintAliasRepository aliases;
	private final PhoneIdentityRepository phones;
	private final UserRepository users;
	private final SocialIdentityRepository socials;
	private final FirebaseIdentityRepository bindings;
	private final UserWithdrawalLifecycleRepository withdrawals;
	private final ProviderChangeGuard guard;
	public RecoveryAccountResolver(PhoneFingerprintHasher hasher, PhoneFingerprintAliasRepository aliases,
			PhoneIdentityRepository phones, UserRepository users, SocialIdentityRepository socials,
			FirebaseIdentityRepository bindings, UserWithdrawalLifecycleRepository withdrawals, ProviderChangeGuard guard) {
		this.hasher=hasher; this.aliases=aliases; this.phones=phones; this.users=users; this.socials=socials;
		this.bindings=bindings; this.withdrawals=withdrawals; this.guard=guard;
	}
	public RecoveryResult resolve(String normalizedPhone) {
		var matches = aliases.findAllActiveByFingerprints(hasher.fingerprint(normalizedPhone).retained());
		var owners = matches.stream().map(a -> a.getUserId()).distinct().toList();
		if (owners.isEmpty()) return RecoveryResult.notFound();
		if (owners.size() != 1) throw new AuthException(AuthErrorStatus.RECOVERY_UNAVAILABLE);
		String id = owners.getFirst();
		var user = users.findById(id).orElse(null);
		if (user == null) throw new AuthException(AuthErrorStatus.RECOVERY_UNAVAILABLE);
		if (user.getStatus() == UserStatus.WITHDRAWN) return RecoveryResult.notFound();
		var phone = phones.findByUserIdAndStatus(id, PhoneIdentityStatus.ACTIVE).orElse(null);
		if (phone == null || matches.stream().anyMatch(a -> !a.getPhoneIdentityId().equals(phone.getPhoneIdentityId()))) {
			throw new AuthException(AuthErrorStatus.RECOVERY_UNAVAILABLE);
		}
		if (!user.isMember() || user.getStatus() != UserStatus.ACTIVE || withdrawals.findByUserId(id).isPresent()) {
			return RecoveryResult.actionRequired();
		}
		var approved = socials.findAllByUserId(id);
		var binding = bindings.findByUserId(id).orElse(null);
		if (approved.size() != 1 || binding == null) return RecoveryResult.actionRequired();
		try {
			if (guard.unresolved(id) || guard.control(id, binding.getFirebaseIdentityId()).isBlocked(approved.getFirst().getProvider())) {
				return RecoveryResult.actionRequired();
			}
		} catch (AuthException e) {
			if (e.getErrorCode() == AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT) return RecoveryResult.actionRequired();
			throw e;
		}
		return RecoveryResult.found(approved.getFirst());
	}
}
