package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

final class WithdrawalTestSupport {
    static final Instant NOW=Instant.parse("2026-10-12T00:00:00Z");
    static final String EVENT="11111111-1111-4111-8111-111111111111";
    static final String USER="22222222-2222-4222-8222-222222222222";
    static final String STREAM="33333333-3333-4333-8333-333333333333";
    static final Clock CLOCK=Clock.fixed(NOW,ZoneOffset.UTC);
    static Journal journal() { return new Journal(EVENT,STREAM,1,EVENT,USER,NOW,NOW); }
    static Delivery delivery(State state, long attempt) {
        return new Delivery(EVENT+":BILLING",EVENT,BILLING,state,0,attempt,attempt,NOW,NOW,"worker",NOW.plusSeconds(60),
                1,null,null,null,null,null,null,null);
    }
    static final class Manager extends AbstractPlatformTransactionManager {
        int commits, rollbacks;
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object tx,TransactionDefinition def) { }
        protected void doCommit(DefaultTransactionStatus s) { commits++; }
        protected void doRollback(DefaultTransactionStatus s) { rollbacks++; }
    }
}
