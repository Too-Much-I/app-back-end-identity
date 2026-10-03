package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.*;
import web.tosunsaeng.identity.domain.auth.domain.EmailHint;
import web.tosunsaeng.identity.domain.auth.federation.repository.*;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.*;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.*;
import web.tosunsaeng.identity.domain.auth.providerchange.*;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.*;

class RecoveryAccountResolverTests {
	String id = "11111111-1111-4111-8111-111111111111";
	PhoneFingerprintHasher hasher = mock(PhoneFingerprintHasher.class);
	PhoneFingerprintAliasRepository aliases = mock(PhoneFingerprintAliasRepository.class);
	PhoneIdentityRepository phones = mock(PhoneIdentityRepository.class);
	UserRepository users = mock(UserRepository.class);
	SocialIdentityRepository socials = mock(SocialIdentityRepository.class);
	FirebaseIdentityRepository bindings = mock(FirebaseIdentityRepository.class);
	UserWithdrawalLifecycleRepository withdrawals = mock(UserWithdrawalLifecycleRepository.class);
	ProviderChangeGuard guard = mock(ProviderChangeGuard.class);
	User user = mock(User.class);
	PhoneFingerprintAlias alias = mock(PhoneFingerprintAlias.class);
	PhoneIdentity phone = mock(PhoneIdentity.class);
	AuthMethodChangeControl control = mock(AuthMethodChangeControl.class);
	RecoveryAccountResolver resolver = new RecoveryAccountResolver(hasher, aliases, phones, users, socials, bindings, withdrawals, guard);
	@BeforeEach void setup() {
		var fp = new PhoneFingerprint("v1", "a".repeat(43));
		when(hasher.fingerprint(anyString())).thenReturn(new PhoneFingerprintSet(fp, List.of(fp)));
		when(aliases.findAllActiveByFingerprints(anyList())).thenReturn(List.of(alias));
		when(alias.getUserId()).thenReturn(id); when(alias.getPhoneIdentityId()).thenReturn("phone-id");
		when(phone.getPhoneIdentityId()).thenReturn("phone-id");
		when(phones.findByUserIdAndStatus(id, PhoneIdentityStatus.ACTIVE)).thenReturn(Optional.of(phone));
		when(users.findById(id)).thenReturn(Optional.of(user)); when(user.isMember()).thenReturn(true); when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(socials.findAllByUserId(id)).thenReturn(List.of(SocialIdentity.create(id, SocialProvider.GOOGLE, "subject", Instant.EPOCH)
				.withEmailHint(EmailHint.from(SocialProvider.GOOGLE, "alice@example.com"), Instant.EPOCH)));
		when(bindings.findByUserId(id)).thenReturn(Optional.of(FirebaseIdentity.create("project", "uid", id, Instant.EPOCH)));
		when(guard.control(eq(id), anyString())).thenReturn(control);
	}
	RecoveryResult resolve() { return resolver.resolve("+16505550123"); }
	@Test void returnsOnlySoleApprovedProviderAndMaskedEmail() {
		var result = resolve();
		assertThat(result.status()).isEqualTo(RecoveryResult.Status.FOUND);
		assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
		assertThat(result.maskedEmail()).isEqualTo("a***@example.com");
	}
	@Test void noAliasOrWithdrawnIsNotFound() {
		when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN);
		assertThat(resolve().status()).isEqualTo(RecoveryResult.Status.NOT_FOUND);
		when(aliases.findAllActiveByFingerprints(anyList())).thenReturn(List.of());
		assertThat(resolve().status()).isEqualTo(RecoveryResult.Status.NOT_FOUND);
	}
	@Test void suspendedUnresolvedOrBlockedNeverRevealsHint() {
		when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);
		assertThat(resolve()).isEqualTo(RecoveryResult.actionRequired());
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE); when(guard.unresolved(id)).thenReturn(true);
		assertThat(resolve()).isEqualTo(RecoveryResult.actionRequired());
		when(guard.unresolved(id)).thenReturn(false); when(control.isBlocked(SocialProvider.GOOGLE)).thenReturn(true);
		assertThat(resolve()).isEqualTo(RecoveryResult.actionRequired());
	}
	@Test void aliasesForDifferentOwnersAreNeverGuessed() {
		var other = mock(PhoneFingerprintAlias.class); when(other.getUserId()).thenReturn("other");
		when(aliases.findAllActiveByFingerprints(anyList())).thenReturn(List.of(alias, other));
		assertThatThrownBy(this::resolve).isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.RECOVERY_UNAVAILABLE));
	}
	@Test void staleSecurityBindingRequiresSupportWithoutHint() {
		when(guard.control(eq(id), anyString())).thenThrow(new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
		assertThat(resolve()).isEqualTo(RecoveryResult.actionRequired());
	}
	@Test void inconsistentPhoneAndMultipleSocialsFailClosed() {
		when(phone.getPhoneIdentityId()).thenReturn("different");
		assertThatThrownBy(this::resolve).isInstanceOf(AuthException.class);
		when(phone.getPhoneIdentityId()).thenReturn("phone-id");
		when(socials.findAllByUserId(id)).thenReturn(List.of());
		assertThat(resolve()).isEqualTo(RecoveryResult.actionRequired());
	}
}
