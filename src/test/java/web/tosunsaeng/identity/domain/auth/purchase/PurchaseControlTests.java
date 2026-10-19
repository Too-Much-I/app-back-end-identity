package web.tosunsaeng.identity.domain.auth.purchase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class PurchaseControlTests {
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    static final String ID = "11111111-1111-4111-8111-111111111111";
    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }
    static User member() {
        User u = User.createFederatedMember("synthetic", UserConsents.unconsented(), NOW);
        ReflectionTestUtils.setField(u, "userId", ID); return u;
    }
    static UserSessionControl control() {
        var c = new UserSessionControl(ID);
        c.initializePurchaseState(UserStatus.ACTIVE, UserAccountType.MEMBER);
        ReflectionTestUtils.setField(c, "version", 1L); return c;
    }

    @Test void migrationInitializationPreservesAllExistingSecurityFields() {
        var c = new UserSessionControl(ID);
        c.logout("synthetic-binding", NOW);
        c.confirmRevocation("synthetic-binding", NOW.plusSeconds(2));
        c.claimLogout("synthetic-operation");
        ReflectionTestUtils.setField(c, "version", 7L);
        long epoch = c.getSessionEpoch();
        var boundary = c.getMinimumFirebaseAuthTimeExclusive();
        c.initializePurchaseState(UserStatus.ACTIVE, UserAccountType.MEMBER);
        assertThat(c.getSessionEpoch()).isEqualTo(epoch);
        assertThat(c.getVersion()).isEqualTo(7L);
        assertThat(c.getActiveLogoutId()).isEqualTo("synthetic-operation");
        assertThat(c.getFirebaseBindingId()).isEqualTo("synthetic-binding");
        assertThat(c.getMinimumFirebaseAuthTimeExclusive()).isEqualTo(boundary);
        assertThat(c.getConfirmedFirebaseRevocationBoundary()).isEqualTo(NOW.plusSeconds(2));
        assertThat(c.isMarkingRequired()).isTrue();
    }

    @Test void approvalExpiryOnlyMovesForwardAndWithdrawalCannotBeReactivated() {
        var c = control();
        c.approvePurchase(NOW.plusSeconds(1800));
        c.approvePurchase(NOW.plusSeconds(100));
        assertThat(c.getLastPurchaseTokenExpiresAt()).isEqualTo(NOW.plusSeconds(1800));
        c.changePurchaseState(UserStatus.WITHDRAWN, UserAccountType.MEMBER);
        assertThat(c.getSessionEpoch()).isEqualTo(1);
        c.changePurchaseState(UserStatus.WITHDRAWN, UserAccountType.MEMBER);
        assertThat(c.getSessionEpoch()).isEqualTo(1);
        assertThatThrownBy(() -> c.approvePurchase(NOW.plusSeconds(2000))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.changePurchaseState(UserStatus.ACTIVE, UserAccountType.MEMBER))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.changePurchaseState(UserStatus.WITHDRAWN, UserAccountType.GUEST))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void guestNeverApprovesPurchaseAndPromotionKeepsEpoch() {
        var c = new UserSessionControl(ID);
        c.initializePurchaseState(UserStatus.ACTIVE, UserAccountType.GUEST);
        assertThatThrownBy(() -> c.approvePurchase(NOW)).isInstanceOf(IllegalStateException.class);
        c.changePurchaseState(UserStatus.ACTIVE, UserAccountType.MEMBER);
        c.approvePurchase(NOW.plusSeconds(1));
        assertThat(c.getSessionEpoch()).isZero();
    }

    @Test void missingControlIsRejectedWithoutInsertAndTransactionIsRequired() {
        var mongo = mock(MongoTemplate.class); var users = mock(UserRepository.class);
        var service = new PurchaseControlService(mongo, users, mock(TransactionTemplate.class));
        assertThatThrownBy(() -> service.validateAndTouch(ID, UserAccountType.MEMBER, NOW)).isInstanceOf(AuthException.class);
        verifyNoInteractions(mongo, users);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        when(users.findById(ID)).thenReturn(Optional.of(member()));
        assertThatThrownBy(() -> service.validateAndTouch(ID, UserAccountType.MEMBER, NOW)).isInstanceOf(AuthException.class);
        verify(mongo, never()).insert(any(UserSessionControl.class));
        verify(mongo, never()).save(any(UserSessionControl.class));
    }

    @ParameterizedTest @EnumSource(UserStatus.class)
    void serverUserStateAndControlMustBothAuthorize(UserStatus state) {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var mongo = mock(MongoTemplate.class); var users = mock(UserRepository.class);
        User u = member(); ReflectionTestUtils.setField(u, "status", state);
        when(users.findById(ID)).thenReturn(Optional.of(u));
        var control = control(); when(mongo.findById(ID, UserSessionControl.class)).thenReturn(control);
        var service = new PurchaseControlService(mongo, users, mock(TransactionTemplate.class));
        if (state == UserStatus.ACTIVE) {
            service.validateAndTouch(ID, UserAccountType.MEMBER, NOW.plusSeconds(30));
            verify(mongo).save(control);
        } else {
            assertThatThrownBy(() -> service.validateAndTouch(ID, UserAccountType.MEMBER, NOW))
                    .isInstanceOf(web.tosunsaeng.identity.global.exception.BusinessException.class);
            verify(mongo, never()).save(any(UserSessionControl.class));
        }
    }

    @Test void terminalMissingControlIsCreatedButActiveTransitionCannotUpsert() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var mongo = mock(MongoTemplate.class);
        var service = new PurchaseControlService(mongo, mock(UserRepository.class), mock(TransactionTemplate.class));
        assertThatThrownBy(() -> service.transition(member())).isInstanceOf(AuthException.class);
        service.transition(member().toWithdrawnTombstone(NOW));
        var capture = org.mockito.ArgumentCaptor.forClass(UserSessionControl.class);
        verify(mongo).insert(capture.capture());
        assertThat(capture.getValue().getPurchaseState()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test void flagsAreOffByDefaultAndActivationRequiresAllGates() {
        var p = new PurchaseProperties();
        assertThat(p.isFenceEnabled() || p.isScopeEnabled() || p.isControlWritesEnabled()).isFalse();
        p.validate(Duration.ofHours(1), false, false);
        p.setScopeEnabled(true);
        p.setSourceStreamId("00000000-0000-4000-8000-000000000001");
        assertThatThrownBy(() -> p.validate(Duration.ofMinutes(30), true, true)).isInstanceOf(IllegalStateException.class);
        p.setControlWritesEnabled(true); p.setFenceEnabled(true);
        assertThatThrownBy(() -> p.validate(Duration.ofMinutes(30), true, false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> p.validate(Duration.ofMinutes(30), false, true)).isInstanceOf(IllegalStateException.class);
        for (Duration ttl : java.util.List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofMinutes(31)))
            assertThatThrownBy(() -> p.validate(ttl, true, true)).isInstanceOf(IllegalStateException.class);
        p.validate(Duration.ofMinutes(30), true, true);
    }
}
