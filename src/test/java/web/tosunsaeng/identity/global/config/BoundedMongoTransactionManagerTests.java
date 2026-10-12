package web.tosunsaeng.identity.global.config;

import com.mongodb.MongoException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BoundedMongoTransactionManagerTests {
    @Test void retriesSameUnknownCommitAtMostThreeTimes() {
        var calls=new AtomicInteger(); var ex=new MongoException("synthetic");
        ex.addLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL);
        assertThatThrownBy(() -> BoundedMongoTransactionManager.retryCommit(() -> {calls.incrementAndGet();throw ex;})).isSameAs(ex);
        assertThat(calls.get()).isEqualTo(3);
    }
    @Test void transientBusinessFailureIsNotRetriedAsCommit() {
        var calls=new AtomicInteger(); var ex=new MongoException("synthetic"); ex.addLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL);
        assertThatThrownBy(() -> BoundedMongoTransactionManager.retryCommit(() -> {calls.incrementAndGet();throw ex;})).isSameAs(ex);
        assertThat(calls.get()).isEqualTo(1);
    }
    @Test void secondCommitCanConfirmSameTransaction() {
        var calls=new AtomicInteger(); var ex=new MongoException("synthetic"); ex.addLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL);
        BoundedMongoTransactionManager.retryCommit(() -> {if(calls.incrementAndGet()==1)throw ex;});
        assertThat(calls.get()).isEqualTo(2);
    }
}
