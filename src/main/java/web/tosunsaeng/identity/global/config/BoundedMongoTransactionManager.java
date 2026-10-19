package web.tosunsaeng.identity.global.config;

import com.mongodb.MongoException;
import com.mongodb.TransactionOptions;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/** Retries only the same commit, never the business operation or sequence allocation. */
final class BoundedMongoTransactionManager extends MongoTransactionManager {
    BoundedMongoTransactionManager(MongoDatabaseFactory factory, TransactionOptions options) {
        super(factory,options); setDefaultTimeout(30);
    }
    @Override protected void doCommit(MongoTransactionObject transaction) throws Exception {
        retryCommit(transaction::commitTransaction);
    }
    static void retryCommit(Runnable commit) {
        long start=System.nanoTime();
        for (int attempt=1;;attempt++) {
            try { commit.run(); return; }
            catch (MongoException failure) {
                if (!failure.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)
                        || attempt >= 3 || System.nanoTime()-start >= java.time.Duration.ofSeconds(15).toNanos()) throw failure;
            }
        }
    }
}
