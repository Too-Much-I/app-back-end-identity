package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class FirebaseGuestMergeTargetResolverTests {

	private static final String SOURCE_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");
	private final FirebaseIdentityRepository firebaseRepository = mock(
			FirebaseIdentityRepository.class
	);
	private final SocialIdentityRepository socialRepository = mock(
			SocialIdentityRepository.class
	);
	private final UserRepository userRepository = mock(UserRepository.class);
	private FirebaseGuestMergeTargetResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new FirebaseGuestMergeTargetResolver(
				firebaseRepository,
				socialRepository,
				userRepository
		);
	}

	@Test
	void resolvesOnlyExistingActiveMemberOwner() {
		User target = member("target");
		when(firebaseRepository.findByFirebaseProjectIdAndFirebaseUid("project", "uid"))
				.thenReturn(Optional.of(FirebaseIdentity.create(
						"project", "uid", target.getUserId(), NOW
				)));
		when(userRepository.findById(target.getUserId())).thenReturn(Optional.of(target));

		assertThat(resolver.resolve(principal(List.of()), SOURCE_ID)).isSameAs(target);
	}

	@Test
	void rejectsUnownedSameOwnerAndMixedOwnershipProofs() {
		when(firebaseRepository.findByFirebaseProjectIdAndFirebaseUid("project", "uid"))
				.thenReturn(Optional.empty());
		assertConflict(() -> resolver.resolve(principal(List.of()), SOURCE_ID));

		when(firebaseRepository.findByFirebaseProjectIdAndFirebaseUid("project", "uid"))
				.thenReturn(Optional.of(FirebaseIdentity.create(
						"project", "uid", SOURCE_ID, NOW
				)));
		assertConflict(() -> resolver.resolve(principal(List.of()), SOURCE_ID));

		User target = member("target");
		User other = member("other");
		VerifiedSocialPrincipal google = new VerifiedSocialPrincipal(
				SocialProvider.GOOGLE,
				"google-subject"
		);
		when(firebaseRepository.findByFirebaseProjectIdAndFirebaseUid("project", "uid"))
				.thenReturn(Optional.of(FirebaseIdentity.create(
						"project", "uid", target.getUserId(), NOW
				)));
		when(socialRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject"
		)).thenReturn(Optional.of(SocialIdentity.create(
				other.getUserId(),
				SocialProvider.GOOGLE,
				"google-subject",
				NOW
		)));
		assertConflict(() -> resolver.resolve(principal(List.of(google)), SOURCE_ID));
	}

	private VerifiedFirebasePrincipal principal(List<VerifiedSocialPrincipal> social) {
		return new VerifiedFirebasePrincipal(
				"project",
				"uid",
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				true,
				false,
				null,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				social
		);
	}

	private User member(String nickname) {
		return User.createFederatedMember(
				nickname,
				UserConsents.unconsented(),
				NOW.minusSeconds(100)
		);
	}

	private void assertConflict(Runnable invocation) {
		AuthException exception = catchThrowableOfType(AuthException.class, invocation::run);
		assertThat(exception.getErrorCode())
				.isEqualTo(AuthErrorStatus.GUEST_MERGE_TARGET_CONFLICT);
	}
}
