package web.tosunsaeng.identity.domain.auth.purchase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;

class PurchaseMigrationTests {
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final PurchaseTokenFenceTests.Manager manager = new PurchaseTokenFenceTests.Manager();
    final PurchaseControlMigration migration = new PurchaseControlMigration(mongo, new TransactionTemplate(manager));
    void seed(User user, UserSessionControl control) {
        when(mongo.find(any(Query.class), eq(User.class))).thenReturn(List.of(user));
        when(mongo.findById(user.getUserId(), User.class)).thenReturn(user);
        when(mongo.findById(user.getUserId(), UserSessionControl.class)).thenReturn(control);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
    }
    @Test void dryRunDoesNotCreateOrModifyAnything() {
        seed(PurchaseControlTests.member(), null);
        var result = migration.batch(null, 10, false);
        assertThat(result.missing()).isEqualTo(1);
        assertThat(result.applied()).isZero();
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(User.class));
        verify(mongo, never()).insert(any(UserSessionControl.class));
        assertThat(manager.commits).isZero();
    }
    @Test void missingControlForWithdrawnUserIsNeverMadeActive() {
        seed(PurchaseControlTests.member().toWithdrawnTombstone(PurchaseControlTests.NOW), null);
        var result = migration.batch(null, 10, true);
        assertThat(result.applied()).isEqualTo(1);
        var capture = org.mockito.ArgumentCaptor.forClass(UserSessionControl.class);
        verify(mongo).insert(capture.capture());
        assertThat(capture.getValue().getPurchaseState()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(manager.commits).isEqualTo(1);
    }
    @Test void migrationPerformsRealUserWriteAndPreservesEpochAndRevocation() {
        var c = new UserSessionControl(PurchaseControlTests.ID);
        c.logout("synthetic-binding", PurchaseControlTests.NOW);
        c.confirmRevocation("synthetic-binding", PurchaseControlTests.NOW.plusSeconds(1));
        c.markingComplete();
        long epoch = c.getSessionEpoch();
        seed(PurchaseControlTests.member(), c);
        assertThat(migration.batch(null, 1, true).applied()).isEqualTo(1);
        assertThat(c.getSessionEpoch()).isEqualTo(epoch);
        assertThat(c.getConfirmedFirebaseRevocationBoundary()).isEqualTo(PurchaseControlTests.NOW.plusSeconds(1));
        var query = org.mockito.ArgumentCaptor.forClass(Query.class);
        var update = org.mockito.ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(query.capture(), update.capture(), eq(User.class));
        assertThat(query.getValue().getQueryObject()).containsKeys("_id", "status", "updatedAt");
        assertThat(update.getValue().getUpdateObject().get("$inc", org.bson.Document.class))
                .containsEntry("purchaseControlRevision", 1L);
        verify(mongo).save(c);
        assertThat(migration.batch(null, 1, true).ready()).isEqualTo(1);
        verify(mongo, times(1)).save(c); // restart is not a destructive reset
    }
    @Test void concurrentUserChangeRollsBackBeforeControlWrite() {
        seed(PurchaseControlTests.member(), null);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(User.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));
        assertThatThrownBy(() -> migration.batch(null, 1, true))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        verify(mongo, never()).insert(any(UserSessionControl.class));
        assertThat(manager.rollbacks).isEqualTo(1);
    }
    @Test void inProgressProtectionIsReportedRatherThanOverwritten() {
        var c = new UserSessionControl(PurchaseControlTests.ID); c.claimLogout("synthetic-operation");
        seed(PurchaseControlTests.member(), c);
        assertThat(migration.batch(null, 1, true).conflicts()).isEqualTo(1);
        verify(mongo, never()).save(any(UserSessionControl.class));
    }
    @Test void mismatchedTerminalControlIsNotRepairedToActive() {
        var c = new UserSessionControl(PurchaseControlTests.ID);
        c.initializePurchaseState(UserStatus.WITHDRAWN, UserAccountType.MEMBER);
        seed(PurchaseControlTests.member(), c);
        assertThat(migration.batch(null, 1, true).conflicts()).isEqualTo(1);
        verify(mongo, never()).save(any(UserSessionControl.class));
    }
    @Test void invalidBatchOrCursorRejectedBeforeDbCall() {
        assertThatThrownBy(() -> migration.batch(null, 101, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> migration.batch(null, 0, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> migration.batch("bad", 1, false)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mongo);
    }
}
