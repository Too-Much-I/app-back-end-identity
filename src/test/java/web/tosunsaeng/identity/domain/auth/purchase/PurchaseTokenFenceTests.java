package web.tosunsaeng.identity.domain.auth.purchase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.oauth2.jwt.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.global.security.jwt.*;

/** Transaction proxy semantics with mocked storage; not a Mongo durability/failover test. */
class PurchaseTokenFenceTests {
    static final Instant NOW = PurchaseControlTests.NOW;
    static final String ID = PurchaseControlTests.ID;
    static class Manager extends AbstractPlatformTransactionManager {
        int commits, rollbacks;
        boolean unknown;
        Runnable afterCommit = () -> { };
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            commits++;
            if (unknown) throw new TransactionSystemException("Synthetic unknown commit");
            afterCommit.run();
        }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
    static class Time extends Clock {
        Instant time = NOW;
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return time; }
    }
    static class Fixture {
        final PurchaseProperties properties = new PurchaseProperties();
        final PurchaseReadiness readiness = mock(PurchaseReadiness.class);
        final PurchaseControlService controls = mock(PurchaseControlService.class);
        final Manager manager = new Manager();
        final Time time = new Time();
        final PurchaseTokenFence fence = new PurchaseTokenFence(properties, readiness, controls,
                new TransactionTemplate(manager), time, mock(RefreshSessionRepository.class), mock(SessionSecurityService.class));
        final AtomicReference<JwtClaimsSet> encoded = new AtomicReference<>();
        final JwtAccessTokenIssuer issuer;
        Fixture() {
            properties.setControlWritesEnabled(true); properties.setFenceEnabled(true); properties.setScopeEnabled(true);
            when(readiness.ready()).thenReturn(true);
            JwtEncoder encoder = input -> {
                encoded.set(input.getClaims());
                return Jwt.withTokenValue("synthetic-not-a-credential").header("alg", "RS256")
                        .claims(claims -> claims.putAll(input.getClaims().getClaims())).build();
            };
            issuer = new JwtAccessTokenIssuer(encoder, new JwtProperties("https://identity.example.test",
                    "tosunsaeng-learning-core", "synthetic-kid", Duration.ofMinutes(30),
                    "unused", "unused", List.of("billing:purchase", "profile:read")), time);
            issuer.setPurchaseFence(fence);
        }
    }
    @Test void trustedMemberGrantAndRefreshWorkCommitTogetherBeforeReturn() {
        var f = new Fixture();
        var result = TokenIssuanceBoundary.execute(f.issuer, () -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            var access = f.issuer.issue(ID, UserAccountType.MEMBER, Set.of());
            assertThat(f.manager.commits).isZero();
            return access;
        });
        assertThat(f.manager.commits).isEqualTo(1);
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(1800));
        assertThat(f.encoded.get().getClaimAsString("scope")).isEqualTo("billing:purchase billing:read profile:read");
        verify(f.controls).validateAndTouch(ID, UserAccountType.MEMBER, NOW.plusSeconds(1800));
    }
    @Test void refreshFailureRollsBackAndDoesNotExposeSignedResult() {
        var f = new Fixture();
        assertThatThrownBy(() -> TokenIssuanceBoundary.execute(f.issuer, () -> {
            f.issuer.issue(ID, UserAccountType.MEMBER, Set.of());
            throw new IllegalStateException("Synthetic refresh save failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(f.manager.commits).isZero();
        assertThat(f.manager.rollbacks).isEqualTo(1);
    }
    @Test void unknownCommitReturns503AndDoesNotRetrySigning() {
        var f = new Fixture(); f.manager.unknown = true;
        assertThatThrownBy(() -> f.issuer.issue(ID, UserAccountType.MEMBER, Set.of()))
                .isInstanceOfSatisfying(AuthException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.AUTH_ISSUANCE_UNRESOLVED));
        assertThat(f.manager.commits).isEqualTo(1);
        verify(f.controls, times(1)).validateAndTouch(anyString(), any(), any());
    }
    @Test void readinessFailureRejectsBeforeSignAndDoesNotPoisonNextRequestContext() {
        var f = new Fixture(); when(f.readiness.ready()).thenReturn(false);
        assertThatThrownBy(() -> f.issuer.issue(ID, UserAccountType.MEMBER, Set.of())).isInstanceOf(AuthException.class);
        assertThat(f.encoded.get()).isNull();
        verifyNoInteractions(f.controls);
        when(f.readiness.ready()).thenReturn(true);
        f.issuer.issue(ID, UserAccountType.MEMBER, Set.of());
        assertThat(f.manager.commits).isEqualTo(1);
    }
    @Test void offScopeStillFencesMemberButCannotGrantInjectedPurchase() {
        var f = new Fixture(); f.properties.setScopeEnabled(false);
        f.issuer.issue(ID, UserAccountType.MEMBER, Set.of("billing:purchase"));
        assertThat(f.encoded.get().getClaimAsString("scope")).isEqualTo("billing:read");
        verify(f.controls).validateAndTouch(ID, UserAccountType.MEMBER, null);
    }
    @Test void guestIssuanceNeverUsesPurchaseAuthorization() {
        var f = new Fixture(); f.issuer.issue(ID, UserAccountType.GUEST, Set.of("billing:purchase"));
        assertThat(f.encoded.get().getClaimAsString("scope")).isEqualTo("billing:read");
        verifyNoInteractions(f.controls);
    }
    @Test void withdrawalWinningBeforeApprovalRejectsTheIssue() {
        var f = new Fixture();
        when(f.controls.validateAndTouch(anyString(), any(), any())).thenThrow(SessionSecurityService.unavailable());
        assertThatThrownBy(() -> f.issuer.issue(ID, UserAccountType.MEMBER, Set.of())).isInstanceOf(AuthException.class);
        assertThat(f.encoded.get()).isNull();
        assertThat(f.manager.commits).isZero();
    }
    @Test void expiryDuringCommitDoesNotReturnSuccessOrResign() {
        var f = new Fixture(); f.manager.afterCommit = () -> f.time.time = NOW.plusSeconds(1800);
        assertThatThrownBy(() -> f.issuer.issue(ID, UserAccountType.MEMBER, Set.of())).isInstanceOf(AuthException.class);
        assertThat(f.manager.commits).isEqualTo(1);
        assertThat(f.encoded.get().getExpiresAt()).isEqualTo(NOW.plusSeconds(1800));
    }
    @Test void outerTransactionCommitRemainsOwnedByRecovery() {
        var f = new Fixture(); f.manager.unknown = true;
        assertThatThrownBy(() -> new TransactionTemplate(f.manager).execute(status ->
                f.issuer.issue(ID, UserAccountType.MEMBER, Set.of())))
                .isInstanceOf(TransactionSystemException.class); // Existing recovery sees commit failure.
        assertThat(f.manager.commits).isEqualTo(1);
    }
    @Test void replayDoesNotSignOrExtendAndStillFencesWhenNewGrantIsOff() {
        var f = new Fixture(); f.properties.setScopeEnabled(false);
        Instant originalExpiry = NOW.plusSeconds(20);
        String response = f.fence.execute(() -> {
            f.fence.replay(ID, UserAccountType.MEMBER, originalExpiry, true);
            return "original-synthetic-response";
        });
        assertThat(response).isEqualTo("original-synthetic-response");
        assertThat(f.encoded.get()).isNull();
        verify(f.controls).validateAndTouch(ID, UserAccountType.MEMBER, originalExpiry);
    }
    @Test void replayAfterExpiryFailsWithoutSigning() {
        var f = new Fixture();
        assertThatThrownBy(() -> f.fence.execute(() -> {
            f.fence.replay(ID, UserAccountType.MEMBER, NOW, true); return "never-returned";
        })).isInstanceOf(AuthException.class);
        assertThat(f.manager.commits).isZero();
        assertThat(f.encoded.get()).isNull();
    }
    @Test void exactLowercaseIsRequiredForPurchaseApproval() {
        var f = new Fixture();
        assertThatThrownBy(() -> f.issuer.issue("AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA", UserAccountType.MEMBER, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(f.controls);
    }
}
