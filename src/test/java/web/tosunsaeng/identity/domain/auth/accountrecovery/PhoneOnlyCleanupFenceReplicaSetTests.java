package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import com.mongodb.client.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PHONE_CLEANUP_LOCAL_MONGO", matches = "true")
class PhoneOnlyCleanupFenceReplicaSetTests {
    MongoClient client;
    MongoTemplate mongo;
    PhoneOnlyCleanupFence fence;
    String database;
    @BeforeEach void setup() {
        client = MongoClients.create("mongodb://127.0.0.1:27049/?replicaSet=phone-cleanup-test&directConnection=true&serverSelectionTimeoutMS=5000");
        database = "phone_cleanup_test_" + UUID.randomUUID().toString().replace("-", "");
        var factory = new SimpleMongoClientDatabaseFactory(client, database);
        mongo = new MongoTemplate(factory);
        mongo.createCollection(PhoneOnlyCleanupFence.COLLECTION);
        mongo.createCollection("synthetic_enrollments");
        fence = new PhoneOnlyCleanupFence(mongo, new TransactionTemplate(new MongoTransactionManager(factory)),
                Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneOffset.UTC), "synthetic");
    }
    @AfterEach void cleanup() {
        if (client != null) {
            if (database != null && database.matches("phone_cleanup_test_[0-9a-f]{32}")) client.getDatabase(database).drop();
            client.close();
        }
    }
    boolean unowned() { return !mongo.exists(new Query(), "synthetic_enrollments"); }
    void enroll() { fence.enroll("project", "uid", () -> mongo.insert(new Document("_id", "enrollment"), "synthetic_enrollments")); }

    @Test void deletionFirstPreventsEnrollmentAndRepeatedClaim() {
        assertThat(fence.claim("project", "uid", this::unowned)).isTrue();
        assertThatThrownBy(this::enroll).isInstanceOf(RuntimeException.class);
        assertThat(fence.claim("project", "uid", this::unowned)).isFalse();
        assertThat(unowned()).isTrue();
    }
    @Test void enrollmentFirstPreventsDeletion() {
        enroll();
        assertThat(fence.claim("project", "uid", this::unowned)).isFalse();
    }
    @Test void rollbackDoesNotLeaveBlockedClaim() {
        assertThatThrownBy(() -> fence.enroll("project", "uid", () -> { throw new IllegalStateException("synthetic"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(mongo.count(new Query(), PhoneOnlyCleanupFence.COLLECTION)).isZero();
        assertThat(fence.claim("project", "uid", this::unowned)).isTrue();
    }
    @Test void terminalProofRemainsBlockedUntilRetentionCleanup() {
        assertThat(fence.claim("project", "uid", this::unowned)).isTrue();
        fence.mark("project", "uid", "DELETED", true);
        Document record = mongo.findOne(new Query(), Document.class, PhoneOnlyCleanupFence.COLLECTION);
        assertThat(record.getBoolean("blocked")).isTrue();
        assertThat(record.get("expiresAt")).isNotNull();
        assertThatThrownBy(this::enroll).isInstanceOf(RuntimeException.class);
    }
    @Test void concurrentEnrollmentAndDeletionCannotBothWin() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> enrolled = pool.submit(() -> { start.await(); try { enroll(); return true; } catch (RuntimeException conflict) { return false; } });
            Future<Boolean> claimed = pool.submit(() -> { start.await(); try { return fence.claim("project", "uid", this::unowned); } catch (RuntimeException conflict) { return false; } });
            start.countDown();
            boolean a = enrolled.get(10, TimeUnit.SECONDS), b = claimed.get(10, TimeUnit.SECONDS);
            assertThat(a && b).isFalse();
            assertThat(a || b).isTrue();
        }
    }
}
