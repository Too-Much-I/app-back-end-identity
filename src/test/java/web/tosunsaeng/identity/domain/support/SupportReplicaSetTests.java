package web.tosunsaeng.identity.domain.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import web.tosunsaeng.identity.global.exception.BusinessException;

/** Opt-in only. Fixed localhost, synthetic randomly named database; never uses application infrastructure. */
@EnabledIfEnvironmentVariable(named="SUPPORT_TEST_LOCAL_MONGO", matches="true")
class SupportReplicaSetTests {
    MongoClient client; MongoTemplate mongo; MongoSupportStore store; String database;
    final Instant now = Instant.parse("2026-10-06T00:00:00Z");
    final SupportRequest request = new SupportRequest(SupportRequest.Category.AUTH, "synthetic inquiry contents", null, null);
    final SupportService.Actor actor = new SupportService.Actor(null, "ANONYMOUS");
    @BeforeEach void setup() {
        client = MongoClients.create("mongodb://127.0.0.1:27029/?replicaSet=support-test&directConnection=true&serverSelectionTimeoutMS=5000");
        database = "support_test_" + UUID.randomUUID().toString().replace("-", "");
        var factory = new SimpleMongoClientDatabaseFactory(client, database);
        mongo = new MongoTemplate(factory);
        store = new MongoSupportStore(mongo, new TransactionTemplate(new MongoTransactionManager(factory)));
        store.ensureIndexes();
    }
    @AfterEach void cleanup() {
        if (client != null) {
            if (database != null && database.matches("support_test_[0-9a-f]{32}")) client.getDatabase(database).drop();
            client.close();
        }
    }
    SupportService service(SupportStore selected) {
        return new SupportService(selected, new SupportCrypto(Base64.getEncoder().encodeToString(new byte[32])),
                new ObjectMapper(), Clock.fixed(now, ZoneOffset.UTC), new SimpleMeterRegistry());
    }
    long count(String collection) { return mongo.getCollection(collection).countDocuments(); }
    @Test void failureAfterAllWritesRollsBackInquiryReceiptOutboxAndQuota() {
        MongoSupportStore failing = spy(store);
        doAnswer(i -> { i.callRealMethod(); throw new DataAccessResourceFailureException("synthetic fault"); })
                .when(failing).insert(any(), any(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> service(failing).submit(actor, "127.0.0.1", UUID.randomUUID().toString(), request)).isInstanceOf(BusinessException.class);
        for (String c : List.of(MongoSupportStore.INQUIRIES, MongoSupportStore.REQUESTS, MongoSupportStore.DELIVERIES, MongoSupportStore.QUOTAS)) assertThat(count(c)).isZero();
    }
    @Test void responseLossAfterCommitReplaysOneReceipt() {
        MongoSupportStore uncertain = spy(store); AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(i -> {
            Object value = store.transaction((Supplier<?>)i.getArgument(0));
            if (first.getAndSet(false)) throw new DataAccessResourceFailureException("synthetic lost acknowledgement");
            return value;
        }).when(uncertain).transaction(any());
        var result = service(uncertain).submit(actor, "127.0.0.1", UUID.randomUUID().toString(), request);
        assertThat(result.replay()).isTrue();
        assertThat(count(MongoSupportStore.INQUIRIES)).isEqualTo(1); assertThat(count(MongoSupportStore.DELIVERIES)).isEqualTo(1);
    }
    @Test void parallelIdenticalRequestsCreateOneInquiryAndReplayAfterContention() throws Exception {
        String key = UUID.randomUUID().toString(); var service = service(store);
        try (var executor = Executors.newFixedThreadPool(6)) {
            var start = new CountDownLatch(1); List<Future<?>> futures = new ArrayList<>();
            for (int i=0; i<6; i++) futures.add(executor.submit(() -> {
                start.await(); try { service.submit(actor, "127.0.0.1", key, request); }
                catch (BusinessException e) { assertThat(e.getErrorCode()).isEqualTo(SupportError.SUPPORT_INQUIRY_UNAVAILABLE); }
                return null;
            }));
            start.countDown(); for (var f : futures) f.get(15, TimeUnit.SECONDS);
        }
        assertThat(service.submit(actor, "127.0.0.1", key, request).replay()).isTrue();
        assertThat(count(MongoSupportStore.INQUIRIES)).isEqualTo(1); assertThat(count(MongoSupportStore.REQUESTS)).isEqualTo(1);
        assertThat(count(MongoSupportStore.DELIVERIES)).isEqualTo(1);
    }
    @Test void sixthNewInquiryIsRejectedWithoutPartialWrites() {
        var service = service(store);
        for (int i=0; i<5; i++) service.submit(actor, "127.0.0.1", UUID.randomUUID().toString(), request);
        SupportServiceTests.assertCode(() -> service.submit(actor, "127.0.0.1", UUID.randomUUID().toString(), request), "SUPPORT_INQUIRY_RATE_LIMITED");
        assertThat(count(MongoSupportStore.INQUIRIES)).isEqualTo(5); assertThat(count(MongoSupportStore.REQUESTS)).isEqualTo(5);
    }
    @Test void approvedReplayIsLimitedAndDeletionRemovesRelatedData() {
        var result = service(store).submit(actor, "127.0.0.1", UUID.randomUUID().toString(), request);
        var operation = new SupportOperations(mongo, store);
        var first = store.claim(now);
        assertThatThrownBy(() -> operation.deleteInquiry(result.inquiryId(), "TMI-197", now)).hasMessage("DELIVERY_IN_FLIGHT");
        store.finish(first, "FAILED", now, "PERMANENT", now);
        assertThatThrownBy(() -> operation.replayFailed(result.inquiryId(), 9, "TMI-197", now)).hasMessage("DELIVERY_STATE_CONFLICT");
        assertThat(count(SupportOperations.AUDIT)).isZero();
        operation.replayFailed(result.inquiryId(), 1, "TMI-197", now);
        var second = store.claim(now);
        store.finish(second, "FAILED", now, "PERMANENT", now);
        assertThatThrownBy(() -> operation.replayFailed(result.inquiryId(), 1, "TMI-197", now)).hasMessage("DELIVERY_STATE_CONFLICT");
        operation.deleteInquiry(result.inquiryId(), "TMI-197", now);
        for (String c : List.of(MongoSupportStore.INQUIRIES, MongoSupportStore.REQUESTS, MongoSupportStore.DELIVERIES)) assertThat(count(c)).isZero();
        assertThat(count(SupportOperations.AUDIT)).isEqualTo(2);
    }
    @Test void expiredLeaseCanBeReclaimedAndStaleWorkerCannotComplete() {
        var result = service(store).submit(actor, "127.0.0.1", UUID.randomUUID().toString(), request);
        var first = store.claim(now); assertThat(store.claim(now)).isNull();
        var second = store.claim(now.plusSeconds(61)); assertThat(second.lease()).isNotEqualTo(first.lease());
        store.finish(first, "SENT", now, "OK", now);
        assertThat(mongo.findById(result.inquiryId(), Document.class, MongoSupportStore.DELIVERIES).getString("status")).isEqualTo("SENDING");
        store.finish(second, "SENT", now, "OK", now);
        assertThat(mongo.findById(result.inquiryId(), Document.class, MongoSupportStore.DELIVERIES).getString("status")).isEqualTo("SENT");
    }
}
