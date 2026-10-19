package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

/** All writes participate in the caller's User/control withdrawal transaction. */
public final class WithdrawalCaptureService {
    private final MongoTemplate mongo;
    private final String environment;
    private final Clock clock;
    private final java.util.function.BooleanSupplier ready;
    public WithdrawalCaptureService(MongoTemplate mongo, String environment, Clock clock, java.util.function.BooleanSupplier ready) {
        this.mongo = mongo; this.environment = environment; this.clock = clock;
        this.ready = ready;
    }
    public void capture(UserWithdrawnOutbox event, Instant attemptStarted) {
        requireTransaction();
        if (!ready.getAsBoolean()) throw unavailable();
        checkBudget(attemptStarted);
        Stream stream = mongo.findAndModify(Query.query(Criteria.where("environment").is(environment)
                        .and("status").is("ACTIVE").and("lastSequence").gte(0L).lt(Long.MAX_VALUE)),
                new Update().inc("lastSequence", 1L), FindAndModifyOptions.options().returnNew(true), Stream.class);
        if (stream == null) throw unavailable(); // No runtime stream upsert or overflow wrap.
        mongo.insert(new Journal(event.getEventId(), stream.streamId(), stream.lastSequence(),
                event.getEventId(), event.getUserId(), event.getWithdrawnAt(), attemptStarted));
        mongo.insert(new Delivery(event.getEventId() + ":BILLING", event.getEventId(), BILLING, State.PENDING,
                0, 0, 0, attemptStarted, attemptStarted, null, null, 0, null, null, null, null, null, null, null));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void beforeCommit(boolean readOnly) { checkBudget(attemptStarted); }
            @Override public void afterCommit() { checkBudget(attemptStarted); }
        });
    }
    private void checkBudget(Instant start) {
        Duration elapsed = Duration.between(start, clock.instant());
        if (elapsed.isNegative() || elapsed.compareTo(Duration.ofSeconds(30)) >= 0) throw unavailable();
    }
    public static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw unavailable();
    }
    public static UserException unavailable() {
        return new UserException(UserErrorStatus.WITHDRAWAL_CAPTURE_UNAVAILABLE);
    }
}
