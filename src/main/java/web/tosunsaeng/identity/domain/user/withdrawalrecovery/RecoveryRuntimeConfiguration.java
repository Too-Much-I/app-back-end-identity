package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.Clock;
import java.util.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.CaptureReadiness;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalCaptureProperties;

/** OFF by default. Does not create collections, indexes, keys, or migration evidence. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.withdrawal-recovery", name = "runtime-enabled", havingValue = "true")
public class RecoveryRuntimeConfiguration {
    record Runtime(MongoSnapshotStore store, MongoRecoveryReader reader, RecoveryCursor cursor) { }

    @Bean Runtime recoveryRuntime(MongoTemplate mongo, MongoClient client, Clock clock,
            WithdrawalCaptureProperties capture, CaptureReadiness readiness,
            @Value("${app.withdrawal-recovery.cursor-active-key:}") String active,
            @Value("${app.withdrawal-recovery.cursor-keyring-json:}") String keys) {
        var cursor = cursor(keys, active, clock);
        new RecoveryCursor.Owner(capture.getEnvironment(), "billing");
        if (!capture.isRequired() || !readiness.ready())
            throw new IllegalStateException("Recovery requires ready capture database/configuration.");
        var indexes = new RecoveryIndexes(mongo);
        indexes.verify(); indexes.verifyMigration(capture.getEnvironment());
        var tx = new TransactionTemplate(new MongoTransactionManager(mongo.getMongoDatabaseFactory(),
                TransactionOptions.builder().readConcern(ReadConcern.SNAPSHOT).writeConcern(WriteConcern.MAJORITY)
                        .readPreference(ReadPreference.primary()).build()));
        tx.setTimeout(30);
        var store = new MongoSnapshotStore(mongo, tx, clock, readiness::ready);
        var reader = new MongoRecoveryReader(client, mongo.getDb().withReadPreference(ReadPreference.primary()), clock,
                readiness::ready);
        return new Runtime(store, reader, cursor);
    }
    /** Dedicated recovery keys only. Never propagate parser causes containing supplied key material. */
    static RecoveryCursor cursor(String json, String active, Clock clock) {
        try {
            if (json == null || json.length() > 4096) throw new IllegalArgumentException();
            var node = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json);
            if (node == null || !node.isObject() || node.size() < 1 || node.size() > 8) throw new IllegalArgumentException();
            var keys = new HashMap<String, byte[]>();
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next(); if (!field.getValue().isTextual()) throw new IllegalArgumentException();
                keys.put(field.getKey(), Base64.getDecoder().decode(field.getValue().textValue()));
            }
            return new RecoveryCursor(keys, active, clock);
        } catch (Exception failure) { throw new IllegalStateException("Recovery cursor key configuration is invalid."); }
    }
    @Bean RecoverySnapshotService recoverySnapshotService(Runtime runtime) {
        return new RecoverySnapshotService(runtime.store(), runtime.cursor());
    }
    @Bean RecoveryFeedService recoveryFeedService(Runtime runtime) {
        return new RecoveryFeedService(runtime.reader()::feed, runtime.cursor());
    }
    @Bean SnapshotBuilder recoverySnapshotBuilder(Runtime runtime) { return new SnapshotBuilder(runtime.store(), runtime.reader()::snapshot); }
}
