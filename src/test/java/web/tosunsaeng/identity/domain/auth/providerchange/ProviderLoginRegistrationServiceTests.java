package web.tosunsaeng.identity.domain.auth.providerchange;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.*;
import web.tosunsaeng.identity.domain.auth.federation.repository.*;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

/** Mock repositories/transaction boundary: no Atlas or OAuth traffic, not a replica-set concurrency test. */
class ProviderLoginRegistrationServiceTests {
	static final String USER = "00000000-0000-4000-8000-000000000001";
	static final String OTHER = "00000000-0000-4000-8000-000000000002";
	static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
	MongoTemplate mongo = mock(MongoTemplate.class);
	FirebaseIdentityRepository identities = mock(FirebaseIdentityRepository.class);
	SocialIdentityRepository socials = mock(SocialIdentityRepository.class);
	UserRepository users = mock(UserRepository.class);
	TransactionTemplate tx = mock(TransactionTemplate.class);
	User user = mock(User.class);
	FirebaseIdentity binding = FirebaseIdentity.create("test-project", "test-uid", USER, NOW.minusSeconds(3600));
	AuthMethodChangeControl methods = new AuthMethodChangeControl(USER, binding.getFirebaseIdentityId());
	UserSessionControl control = new UserSessionControl(USER);
	ProviderChangeGuard guard = new ProviderChangeGuard(mongo);
	SessionSecurityService security = new SessionSecurityService(mongo, tx, users, identities);
	ProviderLoginRegistrationService service;
	List<SocialIdentity> stored = new ArrayList<>();
	boolean issued;

	@BeforeEach void setup() {
		when(tx.execute(any())).thenAnswer(i -> ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(new SimpleTransactionStatus()));
		when(users.findById(USER)).thenReturn(Optional.of(user));
		when(user.isMember()).thenReturn(true);
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(identities.findByFirebaseProjectIdAndFirebaseUid("test-project", "test-uid")).thenReturn(Optional.of(binding));
		when(identities.findById(binding.getFirebaseIdentityId())).thenReturn(Optional.of(binding));
		when(mongo.findById(USER, UserSessionControl.class)).thenReturn(control);
		when(mongo.findById(USER, AuthMethodChangeControl.class)).thenReturn(methods);
		when(socials.findAllByUserId(USER)).thenAnswer(i -> List.copyOf(stored));
		when(socials.findByProviderAndProviderSubject(any(), anyString())).thenAnswer(i -> stored.stream()
				.filter(s -> s.getProvider() == i.getArgument(0) && s.getProviderSubject().equals(i.getArgument(1))).findFirst());
		when(socials.save(any())).thenAnswer(i -> { SocialIdentity s = i.getArgument(0); stored.add(s); return s; });
		security.setProviderChanges(guard);
		service = new ProviderLoginRegistrationService(mongo, security, guard, identities, socials, users, Clock.fixed(NOW, ZoneOffset.UTC));
	}
	VerifiedFirebasePrincipal proof(SocialProvider provider) {
		return new VerifiedFirebasePrincipal("test-project", "test-uid", FirebaseAuthenticationMethod.valueOf(provider.name()),
				NOW.minusSeconds(10), NOW, NOW.plusSeconds(3600), true, false, null,
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.APPLE, FirebaseAuthenticationMethod.KAKAO),
				List.of(new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "google-subject"), new VerifiedSocialPrincipal(SocialProvider.APPLE, "apple-subject"),
						new VerifiedSocialPrincipal(SocialProvider.KAKAO, "kakao-subject")));
	}
	String login(SocialProvider provider) { return service.authenticate(binding, proof(provider), () -> { issued = true; return "authenticated"; }); }
	void denied(AuthErrorStatus status) {
		denied(SocialProvider.APPLE, status);
	}
	void denied(SocialProvider provider, AuthErrorStatus status) {
		assertThatThrownBy(() -> login(provider)).isInstanceOfSatisfying(AuthException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(status));
		assertThat(issued).isFalse();
		verify(socials, never()).save(any());
	}

	@ParameterizedTest @EnumSource(value = SocialProvider.class, names = {"GOOGLE", "APPLE", "KAKAO"})
	void firstLoginRegistersOnlyCurrentProviderAndRepeatedLoginConverges(SocialProvider provider) {
		assertThat(login(provider)).isEqualTo("authenticated");
		assertThat(login(provider)).isEqualTo("authenticated");
		assertThat(stored).hasSize(1);
		assertThat(stored.getFirst().getProvider()).isEqualTo(provider);
		assertThat(stored.getFirst().getUserId()).isEqualTo(USER);
		assertThat(methods.getRevision()).isEqualTo(1);
		verify(socials, times(1)).save(any());
		verify(tx, times(2)).execute(any());
	}
	@ParameterizedTest @EnumSource(value = SocialProvider.class, names = {"GOOGLE", "APPLE", "KAKAO"})
	void preservesExistingOtherProvider(SocialProvider target) {
		SocialProvider previous = target == SocialProvider.APPLE ? SocialProvider.GOOGLE : SocialProvider.APPLE;
		stored.add(SocialIdentity.create(USER, previous, previous.name().toLowerCase() + "-subject", NOW));
		login(target);
		assertThat(stored).hasSize(2).extracting(SocialIdentity::getUserId).containsOnly(USER);
	}
	@Test void blockedProviderIsNeverReleased() { methods.block(SocialProvider.APPLE, NOW.minusSeconds(20)); denied(AuthErrorStatus.PROVIDER_RELINK_REQUIRED); }
	@ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {"blocked", "history", "other-owner", "replacement", "pending", "guest", "withdrawn", "missing-subject"})
	void kakaoFailsClosedAtSecurityBoundaries(String scenario) {
		AuthErrorStatus expected = AuthErrorStatus.PROVIDER_RELINK_REQUIRED;
		switch (scenario) {
			case "blocked" -> methods.block(SocialProvider.KAKAO, NOW.minusSeconds(20));
			case "history" -> methods.release(SocialProvider.KAKAO, NOW.minusSeconds(20));
			case "other-owner" -> {
				stored.add(SocialIdentity.create(OTHER, SocialProvider.KAKAO, "kakao-subject", NOW));
				expected = AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT;
			}
			case "replacement" -> stored.add(SocialIdentity.create(USER, SocialProvider.KAKAO, "different-subject", NOW));
			case "pending" -> {
				when(mongo.exists(any(), eq(ProviderLinkAttempt.class))).thenReturn(true);
				expected = AuthErrorStatus.PROVIDER_CHANGE_CONFLICT;
			}
			case "guest" -> { when(user.isMember()).thenReturn(false); expected = AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT; }
			case "withdrawn" -> { when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN); expected = AuthErrorStatus.ACCOUNT_WITHDRAWN; }
			case "missing-subject" -> {
				var original = proof(SocialProvider.KAKAO);
				var missing = new VerifiedFirebasePrincipal(original.firebaseProjectId(), original.firebaseUid(), original.signInMethod(),
						original.authTime(), NOW, NOW.plusSeconds(3600), true, false, null,
						Set.of(FirebaseAuthenticationMethod.KAKAO), List.of());
				assertThatThrownBy(() -> service.authenticate(binding, missing, () -> { issued = true; return "unexpected"; }))
						.isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.PROVIDER_RELINK_REQUIRED));
				assertThat(issued).isFalse(); verify(socials, never()).save(any()); return;
			}
			default -> throw new AssertionError(scenario);
		}
		denied(SocialProvider.KAKAO, expected);
	}
	@Test void historicalProviderIsNotFirstRegistration() { methods.release(SocialProvider.APPLE, NOW.minusSeconds(20)); denied(AuthErrorStatus.PROVIDER_RELINK_REQUIRED); }
	@Test void otherOwnerDenied() {
		stored.add(SocialIdentity.create(OTHER, SocialProvider.APPLE, "apple-subject", NOW));
		denied(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
	}
	@Test void sameProviderReplacementDenied() {
		stored.add(SocialIdentity.create(USER, SocialProvider.APPLE, "different-subject", NOW));
		denied(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
	}
	@Test void occupiedSecuritySlotDenied() { control.claimLogout("pending-operation"); denied(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT); }
	@Test void unresolvedOperationDenied() {
		when(mongo.exists(any(), eq(ProviderLinkAttempt.class))).thenReturn(true);
		denied(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT);
	}
	@Test void guestDenied() { when(user.isMember()).thenReturn(false); denied(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT); }
	@Test void changedFirebaseUidCannotMergeByEmail() {
		when(identities.findByFirebaseProjectIdAndFirebaseUid("test-project", "test-uid")).thenReturn(Optional.empty());
		denied(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
	}
	@Test void replacedBindingDenied() {
		when(identities.findByFirebaseProjectIdAndFirebaseUid("test-project", "test-uid"))
				.thenReturn(Optional.of(FirebaseIdentity.create("test-project", "test-uid", USER, NOW)));
		denied(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
	}
	@Test void withdrawnAccountDeniedBeforeRegistration() { when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN); denied(AuthErrorStatus.ACCOUNT_WITHDRAWN); }
	@Test void logoutDuringTransactionEntryDenied() {
		doAnswer(i -> {
			control.logout(binding.getFirebaseIdentityId(), NOW);
			return ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(new SimpleTransactionStatus());
		}).when(tx).execute(any());
		denied(AuthErrorStatus.SESSION_LOGGED_OUT);
	}
	@Test void unlinkDuringTransactionEntryDenied() {
		doAnswer(i -> {
			methods.block(SocialProvider.APPLE, NOW);
			return ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(new SimpleTransactionStatus());
		}).when(tx).execute(any());
		denied(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
	}
	@Test void optimisticConflictOnSharedSecurityControlFailsClosed() {
		when(mongo.save(any(UserSessionControl.class))).thenThrow(new OptimisticLockingFailureException("test-only conflict"));
		denied(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE);
	}
	@Test void duplicateAtCommitNeverReturnsIssuedResponse() {
		doThrow(new DuplicateKeyException("test-only conflict")).when(tx).execute(any());
		denied(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT);
	}
	@ParameterizedTest @EnumSource(SocialProvider.class)
	void guardDefersMissingProviderOnlyForLoginAndStillEnforcesBlocks(SocialProvider provider) {
		when(mongo.findOne(any(), eq(FirebaseIdentity.class))).thenReturn(binding);
		assertThatThrownBy(() -> guard.validatePrincipal(proof(provider)))
				.isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.PROVIDER_RELINK_REQUIRED));
		guard.validatePrincipal(proof(provider), true);
		methods.block(provider, NOW);
		assertThatThrownBy(() -> guard.validatePrincipal(proof(provider), true))
				.isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.PROVIDER_RELINK_REQUIRED));
		verify(socials, never()).save(any());
	}
	@Test void issuanceFailureEscapesTransactionInsteadOfReturningSuccess() {
		assertThatThrownBy(() -> service.authenticate(binding, proof(SocialProvider.APPLE),
				() -> { throw new AuthException(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE); }))
				.isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE));
		verify(tx).execute(any()); // Rollback is supplied by the production Mongo transaction manager.
	}
}
