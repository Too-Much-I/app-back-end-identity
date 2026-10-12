package web.tosunsaeng.identity.domain.auth.purchase;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

/** Participates in the existing Mongo transaction; never creates ACTIVE state on the read path. */
public final class PurchaseControlService {
    private final MongoTemplate mongo;
    private final UserRepository users;
    private final org.springframework.transaction.support.TransactionTemplate transactions;
    public PurchaseControlService(MongoTemplate mongo, UserRepository users,
            org.springframework.transaction.support.TransactionTemplate transactions) {
        this.mongo = Objects.requireNonNull(mongo); this.users = Objects.requireNonNull(users);
        this.transactions = Objects.requireNonNull(transactions);
    }
    public User register(User user) {
        try { return transactions.execute(status -> {
            User saved = users.save(user);
            initializeNew(saved);
            return saved;
        }); }
        catch (org.springframework.dao.DuplicateKeyException conflict) { throw conflict; }
        catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException failure) {
            throw new web.tosunsaeng.identity.domain.auth.common.exception.AuthException(
                    web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.AUTH_ISSUANCE_UNRESOLVED);
        }
    }
    static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw SessionSecurityService.unavailable();
    }
    public void initializeNew(User user) {
        requireTransaction();
        var control = new UserSessionControl(user.getUserId());
        control.initializePurchaseState(user.getStatus(), user.getAccountType());
        mongo.insert(control); // Duplicate is a conflict, not permission to overwrite security state.
    }
    public void transition(User user) {
        requireTransaction();
        var control = mongo.findById(user.getUserId(), UserSessionControl.class);
        if (control == null) {
            if (user.getStatus() == UserStatus.ACTIVE) throw SessionSecurityService.unavailable();
            control = new UserSessionControl(user.getUserId());
            control.changePurchaseState(user.getStatus(), user.getAccountType());
            mongo.insert(control);
        } else {
            control.changePurchaseState(user.getStatus(), user.getAccountType());
            mongo.save(control);
        }
    }
    public User validateAndTouch(String userId, UserAccountType expectedType, Instant purchaseExpiry) {
        requireTransaction();
        User user = users.findById(userId).orElseThrow(SessionSecurityService::unavailable);
        if (user.getStatus() == UserStatus.WITHDRAWN) throw new web.tosunsaeng.identity.domain.auth.common.exception.AuthException(
                web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.ACCOUNT_WITHDRAWN);
        if (user.getStatus() != UserStatus.ACTIVE) throw new web.tosunsaeng.identity.domain.user.exception.UserException(
                web.tosunsaeng.identity.domain.user.exception.UserErrorStatus.ACCOUNT_NOT_ACTIVE);
        if (user.getAccountType() != expectedType) throw SessionSecurityService.unavailable();
        var control = mongo.findById(userId, UserSessionControl.class);
        if (control == null || control.getVersion() == null || control.getPurchaseState() != user.getStatus()
                || control.getPurchaseAccountType() != user.getAccountType()) throw SessionSecurityService.unavailable();
        if (purchaseExpiry != null) control.approvePurchase(purchaseExpiry);
        else control.touch();
        mongo.save(control);
        return user;
    }
}
