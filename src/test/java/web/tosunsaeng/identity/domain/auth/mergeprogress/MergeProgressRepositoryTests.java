package web.tosunsaeng.identity.domain.auth.mergeprogress;

import java.time.*;
import java.util.*;
import com.mongodb.client.*;
import de.bwaldvogel.mongo.*;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.dao.OptimisticLockingFailureException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.*;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.*;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.*;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.exception.BusinessException;
import static web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressTests.*;
import static web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.*;

class MergeProgressRepositoryTests {
    MongoServer server;
    MongoClient client;
    MongoTemplate mongo;
    UserMergeProgressRepository progress;
    OwnerEventCoreRepository cores;
    OwnerEventDeliveryRepository deliveries;
    OwnerEventConsumerStateRepository states;
    MergeQuerySupport support;
    UserRepository users;
    UserMergeQueryService query;
    OwnerEventCaptureService capture;
    MergeProgressStore store;
    web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties properties;

    @BeforeEach void setUp() {
        server = new MongoServer(new MemoryBackend());
        var address = server.bind();
        client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
        mongo = new MongoTemplate(client, "merge-progress-test");
        var factory = new MongoRepositoryFactory(mongo);
        progress = factory.getRepository(UserMergeProgressRepository.class);
        cores = factory.getRepository(OwnerEventCoreRepository.class, RepositoryFragments.just(new OwnerEventCoreRepositoryCustomImpl(mongo)));
        deliveries = factory.getRepository(OwnerEventDeliveryRepository.class, RepositoryFragments.just(new OwnerEventDeliveryRepositoryCustomImpl(mongo)));
        states = factory.getRepository(OwnerEventConsumerStateRepository.class, RepositoryFragments.just(new OwnerEventConsumerStateRepositoryCustomImpl(mongo)));
        for (var type : List.of(UserMergeProgress.class, MergeQueryCursor.class, MergeQueryQuota.class, OwnerEventCore.class, OwnerEventDelivery.class))
            new MongoPersistentEntityIndexResolver(mongo.getConverter().getMappingContext()).resolveIndexFor(type)
                    .forEach(index -> mongo.indexOps(type).ensureIndex(index));
        support = new MergeQuerySupport(mongo);
        users = mock(UserRepository.class);
        var current = mock(CurrentUserProvider.class);
        when(current.getCurrentUserId()).thenReturn(TARGET);
        var member = mock(User.class);
        when(member.isMember()).thenReturn(true);
        when(member.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(users.findById(TARGET)).thenReturn(Optional.of(member));
        properties = lcProperties();
        query = new UserMergeQueryService(current, users, mongo, support, deliveries, cores, states, properties, Clock.fixed(NOW, ZoneOffset.UTC));
        store = new MergeProgressStore(progress, deliveries);
        capture = new OwnerEventCaptureService(cores, deliveries, states);
        capture.configureProgress(store, properties);
    }
    @AfterEach void stop() { client.close(); server.shutdownNow(); }

    @Test void lcOnlyCaptureAndAckEndToEndSurvivesCoreAndDeliveryTtl() {
        var event = capture.captureUserMerged(SOURCE, TARGET, NOW);
        assertThat(states.findById(OwnerEventConsumer.BILLING)).isEmpty();
        assertThat(query.get(event.getEventId()).status()).isEqualTo(Status.PROCESSING);
        assertThat(query.get(event.getEventId()).billing().status()).isEqualTo(ComponentStatus.NOT_REQUIRED);
        var tx = new OwnerEventPublishTransactionService(deliveries, states, cores, mock(PhoneRejoinLineageRepository.class));
        tx.configureProgress(store);
        var claimed = deliveries.claimExact(OwnerEventConsumer.LEARNING_CORE, 1, "worker", NOW, NOW.plusSeconds(60)).orElseThrow();
        assertThat(tx.complete(claimed, "worker", NOW.plusSeconds(3), NOW.plus(Duration.ofDays(30)))).isTrue();
        cores.deleteAll(); deliveries.deleteAll();
        properties.setMergeCompletionProfile(MergeCompletionProfile.LEARNING_CORE_AND_BILLING);
        properties.setMergeProgressCaptureEnabled(false);
        assertThat(query.get(event.getEventId()).status()).isEqualTo(Status.COMPLETED);
        assertThat(query.get(event.getEventId()).billing().status()).isEqualTo(ComponentStatus.NOT_REQUIRED);
        assertThat(query.list("true", "20", null).items()).isEmpty();
        assertThat(query.list("false", "20", null).items()).hasSize(1);
    }
    @Test void wrongOwnerAndExpiredRowsUseSameNotFoundAndInactiveMemberDenied() {
        var p = UserMergeProgress.create(core(MergeCompletionProfile.LEARNING_CORE_ONLY), MergeCompletionProfile.LEARNING_CORE_ONLY);
        org.springframework.test.util.ReflectionTestUtils.setField(p, "targetUserId", SOURCE);
        progress.insert(p);
        error(() -> query.get(p.getMergeId()), "MERGE_STATUS_NOT_FOUND");
        var expired = UserMergeProgress.create(OwnerEventCore.trackedUserMerged(SOURCE, TARGET, NOW.minus(Duration.ofDays(31)),
                MergeCompletionProfile.LEARNING_CORE_ONLY), MergeCompletionProfile.LEARNING_CORE_ONLY);
        expired.confirm(OwnerEventConsumer.LEARNING_CORE, NOW.minus(Duration.ofDays(30)));
        progress.insert(expired);
        error(() -> query.get(expired.getMergeId()), "MERGE_STATUS_NOT_FOUND");
        when(users.findById(TARGET)).thenReturn(Optional.empty());
        error(() -> query.list("true", "20", null), "ACCOUNT_NOT_ACTIVE");
    }
    @Test void fifoDeadLetterAndDisabledChannelAreActionRequiredAndMissingStateIsUnavailable() {
        // The in-memory Mongo emulator does not implement partial unique index filters.
        // Real replica-set preflight must verify the unchanged trial-only partial index.
        mongo.indexOps(OwnerEventCore.class).dropIndex("uk_owner_event_trial_lineage");
        var first = capture.captureUserMerged(SOURCE, TARGET, NOW);
        var second = capture.captureUserMerged("00000000-0000-4000-8000-000000000003", TARGET, NOW);
        var claim = deliveries.claimExact(OwnerEventConsumer.LEARNING_CORE, 1, "w", NOW, NOW.plusSeconds(60)).orElseThrow();
        deliveries.markDeadLetter(claim.getDeliveryId(), "w", OwnerEventFailureCode.HTTP_400, NOW, NOW.plus(Duration.ofDays(90)));
        assertThat(query.get(second.getEventId()).status()).isEqualTo(Status.ACTION_REQUIRED);
        properties.setLearningCoreUserMergedPublisherEnabled(false);
        assertThat(query.get(first.getEventId()).status()).isEqualTo(Status.ACTION_REQUIRED);
        states.deleteAll();
        error(() -> query.get(second.getEventId()), "MERGE_STATUS_UNAVAILABLE");
    }
    @Test void stablePagingForSameTimestampAndCursorBoundToUserAndFilter() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            var core = OwnerEventCore.trackedUserMerged(UUID.randomUUID().toString(), TARGET, NOW, MergeCompletionProfile.LEARNING_CORE_ONLY);
            var p = UserMergeProgress.create(core, MergeCompletionProfile.LEARNING_CORE_ONLY);
            p.confirm(OwnerEventConsumer.LEARNING_CORE, NOW);
            progress.insert(p); ids.add(p.getMergeId());
        }
        ids.sort(Comparator.reverseOrder());
        var page = query.list("false", "2", null);
        assertThat(page.items()).extracting(MergeProgressResponse::mergeId).containsExactlyElementsOf(ids.subList(0, 2));
        assertThat(query.list("false", "2", page.nextCursor()).items()).extracting(MergeProgressResponse::mergeId).containsExactly(ids.get(2));
        error(() -> query.list("true", "2", page.nextCursor()), "INVALID_MERGE_STATUS_REQUEST");
        error(() -> support.decode(page.nextCursor(), SOURCE, false, NOW), "INVALID_MERGE_STATUS_REQUEST");
        error(() -> support.decode(page.nextCursor(), TARGET, false, NOW.plusSeconds(3600)), "INVALID_MERGE_STATUS_REQUEST");
        error(() -> query.list("false", "51", null), "INVALID_MERGE_STATUS_REQUEST");
        error(() -> query.get("not-a-uuid"), "INVALID_MERGE_STATUS_REQUEST");
    }
    @Test void sharedQuotaAcrossGetAndListAndNextMinuteResets() {
        var p = UserMergeProgress.create(core(MergeCompletionProfile.LEARNING_CORE_ONLY), MergeCompletionProfile.LEARNING_CORE_ONLY);
        p.confirm(OwnerEventConsumer.LEARNING_CORE, NOW); progress.insert(p);
        for (int i = 0; i < 15; i++) { query.get(p.getMergeId()); query.list("false", "20", null); }
        assertThatThrownBy(() -> query.get(p.getMergeId())).isInstanceOf(MergeQuerySupport.RateLimited.class)
                .satisfies(e -> assertThat(((MergeQuerySupport.RateLimited)e).retryAfterSeconds()).isEqualTo(60));
        assertThatCode(() -> support.limit(TARGET, NOW.plusSeconds(60))).doesNotThrowAnyException();
    }
    @Test void concurrentAckUsesCasAndRetryPreservesOtherConsumer() {
        var profile = MergeCompletionProfile.LEARNING_CORE_AND_BILLING;
        var p = progress.insert(UserMergeProgress.create(core(profile), profile));
        var lc = progress.findById(p.getMergeId()).orElseThrow();
        var billing = progress.findById(p.getMergeId()).orElseThrow();
        lc.confirm(OwnerEventConsumer.LEARNING_CORE, NOW.plusSeconds(1)); progress.save(lc);
        billing.confirm(OwnerEventConsumer.BILLING, NOW.plusSeconds(2));
        assertThatThrownBy(() -> progress.save(billing)).isInstanceOf(OptimisticLockingFailureException.class);
        var retry = progress.findById(p.getMergeId()).orElseThrow();
        retry.confirm(OwnerEventConsumer.BILLING, NOW.plusSeconds(3)); progress.save(retry);
        var result = query.get(p.getMergeId());
        assertThat(result.status()).isEqualTo(Status.COMPLETED);
        assertThat(result.learningCore().confirmedAt()).isEqualTo(NOW.plusSeconds(1));
    }
    @Test void privacyReleaseKeepsPendingReceiptUntilAckThenExpiresImmediately() {
        var event = capture.captureUserMerged(SOURCE, TARGET, NOW);
        var privacy = new MergeProgressPrivacyCleanup(mongo);
        privacy.release(TARGET, NOW);
        var pending = progress.findById(event.getEventId()).orElseThrow();
        assertThat(pending.isPrivacyCleanupRequested()).isTrue();
        assertThat(pending.getCleanupAt()).isNull();
        store.confirm(event, OwnerEventConsumer.LEARNING_CORE, NOW);
        assertThat(progress.findById(event.getEventId()).orElseThrow().getCleanupAt()).isEqualTo(NOW);
        error(() -> query.get(event.getEventId()), "MERGE_STATUS_NOT_FOUND");
    }
    @Test void indexesIncludeZeroTtlAndReview() {
        assertThat(mongo.indexOps(UserMergeProgress.class).getIndexInfo())
                .anySatisfy(i -> { assertThat(i.getName()).isEqualTo("ttl_merge_progress"); assertThat(i.getExpireAfter()).hasValue(Duration.ZERO); })
                .anySatisfy(i -> assertThat(i.getName()).isEqualTo("merge_retention_review"));
    }
    static void error(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException)e).getErrorCode().getCode()).isEqualTo(code));
    }
}
