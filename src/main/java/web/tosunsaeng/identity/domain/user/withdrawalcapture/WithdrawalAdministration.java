package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Clock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

/** No REST route. The deployment's separately authorized runner owns barrier proof and durable audit. */
public final class WithdrawalAdministration {
    public interface Authority {
        void require(String action);
        void verifyWriterBarrier();
        void audit(String action, String operationId);
    }
    private final MongoTemplate mongo;
    private final TransactionTemplate transactions;
    private final Authority authority;
    private final Clock clock;
    private final String environment;
    public WithdrawalAdministration(MongoTemplate mongo, TransactionTemplate transactions, Authority authority,
            Clock clock, String environment) {
        this.mongo=mongo; this.transactions=transactions; this.authority=authority; this.clock=clock; this.environment=environment;
    }
    public void cutover(String newStreamId, String expectedOldStreamId) {
        authority.require(expectedOldStreamId == null ? "INITIAL_CUTOVER" : "RESTORE_CUTOVER");
        uuid(newStreamId);
        if (java.util.UUID.fromString(newStreamId).version() != 4) throw new IllegalArgumentException("New stream requires UUID v4.");
        if (expectedOldStreamId != null) uuid(expectedOldStreamId);
        authority.verifyWriterBarrier();
        new WithdrawalIndexes(mongo).verify();
        transactions.executeWithoutResult(tx -> {
            if (mongo.exists(Query.query(Criteria.where("_id").is(newStreamId)), Stream.class))
                throw new IllegalStateException("Stream already exists; inspect prior cutover outcome.");
            var active = Query.query(Criteria.where("environment").is(environment).and("status").is("ACTIVE"));
            if (expectedOldStreamId == null) {
                if (mongo.exists(Query.query(Criteria.where("environment").is(environment)), Stream.class))
                    throw new IllegalStateException("Existing stream requires restore procedure.");
            } else {
                active.addCriteria(Criteria.where("_id").is(expectedOldStreamId));
                if (mongo.updateFirst(active,new Update().set("status","CLOSED_RESTORE").set("closedAt",clock.instant()),Stream.class)
                        .getModifiedCount() != 1) throw new IllegalStateException("Stream replacement conflict.");
            }
            mongo.insert(new Stream(newStreamId,newStreamId,environment,"ACTIVE",0,clock.instant(),0));
            authority.audit(expectedOldStreamId == null ? "INITIAL_CUTOVER" : "RESTORE_CUTOVER",newStreamId);
        });
    }
}
