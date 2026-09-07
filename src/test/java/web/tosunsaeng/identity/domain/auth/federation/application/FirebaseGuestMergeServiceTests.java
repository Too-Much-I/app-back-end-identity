package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestMergeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

class FirebaseGuestMergeServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

	@Test
	void usesJwtSourceAndFreshFirebaseTargetThenIssuesTargetAccessOnlyAfterCommit() {
		CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
		UserRepository userRepository = mock(UserRepository.class);
		FirebaseAuthenticationVerifier verifier = mock(FirebaseAuthenticationVerifier.class);
		FirebaseGuestMergeTargetResolver resolver = mock(FirebaseGuestMergeTargetResolver.class);
		RefreshSessionIssuer sessionIssuer = mock(RefreshSessionIssuer.class);
		FirebaseGuestMergeTransactionService transactionService = mock(
				FirebaseGuestMergeTransactionService.class
		);
		AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
		FirebaseGuestMergeService service = new FirebaseGuestMergeService(
				currentUserProvider,
				userRepository,
				verifier,
				resolver,
				sessionIssuer,
				transactionService,
				accessTokenIssuer,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		User source = User.createGuest(
				"ddddddddddddddddddddddddddddddddddddddddddd",
				"게스트",
				UserConsents.unconsented(),
				NOW.minusSeconds(100)
		);
		User target = User.createFederatedMember(
				"회원",
				UserConsents.unconsented(),
				NOW.minusSeconds(100)
		);
		VerifiedFirebasePrincipal principal = mock(VerifiedFirebasePrincipal.class);
		RefreshSession targetSession = RefreshSession.create(
				target.getUserId(),
				"target-hash",
				NOW,
				NOW.plusSeconds(3600)
		);
		PreparedRefreshSession prepared = new PreparedRefreshSession(
				"target-refresh",
				targetSession
		);
		IssuedRefreshSession issuedRefresh = new IssuedRefreshSession(
				"target-refresh",
				NOW,
				NOW.plusSeconds(3600)
		);
		IssuedAccessToken issuedAccess = new IssuedAccessToken(
				"target-access",
				"Bearer",
				NOW,
				NOW.plusSeconds(1800),
				1800
		);
		when(currentUserProvider.getCurrentUserId()).thenReturn(source.getUserId());
		when(userRepository.findById(source.getUserId())).thenReturn(Optional.of(source));
		when(verifier.verify("fresh-proof", FirebaseVerificationPurpose.GUEST_MERGE))
				.thenReturn(principal);
		when(resolver.resolve(principal, source.getUserId())).thenReturn(target);
		when(sessionIssuer.prepare(target.getUserId())).thenReturn(prepared);
		when(transactionService.merge(any(), eq(source.getUpdatedAt()), eq(prepared), any(), eq(NOW)))
				.thenReturn(issuedRefresh);
		when(accessTokenIssuer.issue(eq(target.getUserId()), eq(UserAccountType.MEMBER), any())).thenReturn(issuedAccess);

		FirebaseSignupResponse response = service.merge(
				new FirebaseGuestMergeRequest("fresh-proof")
		);

		assertThat(response.accessToken()).isEqualTo("target-access");
		assertThat(response.refreshToken()).isEqualTo("target-refresh");
		verify(resolver).resolve(principal, source.getUserId());
		verify(accessTokenIssuer, never()).issue(eq(source.getUserId()), any(), any());
		InOrder order = inOrder(transactionService, accessTokenIssuer);
		order.verify(transactionService).merge(
				any(), eq(source.getUpdatedAt()), eq(prepared), any(), eq(NOW)
		);
		order.verify(accessTokenIssuer).issue(eq(target.getUserId()), eq(UserAccountType.MEMBER), any());
	}
}
