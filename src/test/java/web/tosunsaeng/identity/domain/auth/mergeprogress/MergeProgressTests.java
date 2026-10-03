package web.tosunsaeng.identity.domain.auth.mergeprogress;

import java.time.*;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.*;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.*;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.*;

class MergeProgressTests {
    static final Instant NOW = Instant.parse("2026-10-03T00:00:00Z");
    static final String SOURCE = "00000000-0000-4000-8000-000000000001";
    static final String TARGET = "00000000-0000-4000-8000-000000000002";
    static OwnerEventCore core(MergeCompletionProfile profile) { return OwnerEventCore.trackedUserMerged(SOURCE, TARGET, NOW, profile); }

    @Test void lcOnlyCompletesWithoutBillingAndDuplicateDoesNotExtendRetention() {
        var p = UserMergeProgress.create(core(MergeCompletionProfile.LEARNING_CORE_ONLY), MergeCompletionProfile.LEARNING_CORE_ONLY);
        assertThat(p.getCleanupAt()).isNull();
        p.confirm(OwnerEventConsumer.LEARNING_CORE, NOW.plusSeconds(3));
        p.confirm(OwnerEventConsumer.LEARNING_CORE, NOW.plusSeconds(30));
        assertThat(p.getCompletedAt()).isEqualTo(NOW.plusSeconds(3));
        assertThat(p.getCleanupAt()).isEqualTo(NOW.plusSeconds(3).plus(Duration.ofDays(30)));
        assertThat(p.getBillingConfirmedAt()).isNull();
        assertThatThrownBy(() -> p.confirm(OwnerEventConsumer.BILLING, NOW.plusSeconds(4))).isInstanceOf(IllegalStateException.class);
    }
    @ParameterizedTest @EnumSource(OwnerEventConsumer.class)
    void bothAckOrdersPreserveFirstConfirmation(OwnerEventConsumer first) {
        var p = UserMergeProgress.create(core(MergeCompletionProfile.LEARNING_CORE_AND_BILLING), MergeCompletionProfile.LEARNING_CORE_AND_BILLING);
        p.confirm(first, NOW.plusSeconds(1));
        assertThat(p.getCompletedAt()).isNull();
        var second = first == OwnerEventConsumer.BILLING ? OwnerEventConsumer.LEARNING_CORE : OwnerEventConsumer.BILLING;
        p.confirm(second, NOW.plusSeconds(2));
        assertThat(p.confirmedAt(first)).isEqualTo(NOW.plusSeconds(1));
        assertThat(p.getCompletedAt()).isEqualTo(NOW.plusSeconds(2));
    }
    @Test void emptyRequiredSetCannotProduceVacuousCompletion() {
        var p = UserMergeProgress.create(core(MergeCompletionProfile.LEARNING_CORE_ONLY), MergeCompletionProfile.LEARNING_CORE_ONLY);
        ReflectionTestUtils.setField(p, "requiredConsumers", Set.of());
        assertThatThrownBy(p::validate).isInstanceOf(IllegalStateException.class);
    }
    @Test void trackedCaptureAllocatesOnlyRequiredConsumerAndSavesActualEvent() {
        var cores = mock(OwnerEventCoreRepository.class);
        var deliveries = mock(OwnerEventDeliveryRepository.class);
        var states = mock(OwnerEventConsumerStateRepository.class);
        var store = mock(MergeProgressStore.class);
        var props = lcProperties();
        var capture = new OwnerEventCaptureService(cores, deliveries, states);
        capture.configureProgress(store, props);
        when(states.allocateNext(OwnerEventConsumer.LEARNING_CORE, NOW)).thenReturn(1L);
        var event = capture.captureUserMerged(SOURCE, TARGET, NOW);
        verify(states, never()).allocateNext(eq(OwnerEventConsumer.BILLING), any());
        verify(store).capture(event, MergeCompletionProfile.LEARNING_CORE_ONLY);
        assertThat(event.getRequiredConsumers()).containsExactly(OwnerEventConsumer.LEARNING_CORE);
        props.setMergeCompletionProfile(MergeCompletionProfile.LEARNING_CORE_AND_BILLING);
        assertThat(event.getRequiredConsumers()).containsExactly(OwnerEventConsumer.LEARNING_CORE);
    }
    @Test void explicitProfileAndChannelRequiredButBillingConfigurationNotRequired() {
        var props = lcProperties();
        assertThatCode(props::validateMergeProgress).doesNotThrowAnyException();
        props.setMergeCompletionProfile(null);
        assertThatThrownBy(props::validateMergeProgress).isInstanceOf(IllegalArgumentException.class);
        props.setMergeCompletionProfile(MergeCompletionProfile.LEARNING_CORE_AND_BILLING);
        assertThatThrownBy(props::validateMergeProgress).isInstanceOf(IllegalArgumentException.class);
        props.setMergeCompletionProfile(MergeCompletionProfile.LEARNING_CORE_ONLY);
        props.setLearningCoreUserMergedPublisherEnabled(false);
        assertThatThrownBy(props::validateMergeProgress).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void missingDeliveryCannotBeConfirmedAndMissingSummaryCannotBeBackfilled() {
        var repo = mock(UserMergeProgressRepository.class);
        var deliveries = mock(OwnerEventDeliveryRepository.class);
        var store = new MergeProgressStore(repo, deliveries);
        var event = core(MergeCompletionProfile.LEARNING_CORE_ONLY);
        when(deliveries.findAllByEventId(event.getEventId())).thenReturn(List.of());
        assertThatThrownBy(() -> store.confirm(event, OwnerEventConsumer.LEARNING_CORE, NOW)).isInstanceOf(IllegalStateException.class);
        when(deliveries.findAllByEventId(event.getEventId())).thenReturn(List.of(
                OwnerEventDelivery.create(event.getEventId(), OwnerEventConsumer.LEARNING_CORE, 1, NOW)));
        assertThatThrownBy(() -> store.confirm(event, OwnerEventConsumer.LEARNING_CORE, NOW)).isInstanceOf(IllegalStateException.class);
        verify(repo, never()).insert(any(UserMergeProgress.class));
    }
    @Test void leaseLoserDoesNotConfirmAndStoreFailurePropagatesForTransactionRollback() {
        var deliveries = mock(OwnerEventDeliveryRepository.class);
        var states = mock(OwnerEventConsumerStateRepository.class);
        var cores = mock(OwnerEventCoreRepository.class);
        var store = mock(MergeProgressStore.class);
        var service = new OwnerEventPublishTransactionService(deliveries, states, cores, mock(PhoneRejoinLineageRepository.class));
        service.configureProgress(store);
        var event = core(MergeCompletionProfile.LEARNING_CORE_ONLY);
        var delivery = OwnerEventDelivery.create(event.getEventId(), OwnerEventConsumer.LEARNING_CORE, 1, NOW);
        assertThat(service.complete(delivery, "worker", NOW, NOW.plusSeconds(30))).isFalse();
        verifyNoInteractions(store);
        when(deliveries.markPublished(any(), any(), any(), any())).thenReturn(true);
        when(states.advancePublished(any(), anyLong(), any())).thenReturn(true);
        when(cores.findById(event.getEventId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("storage unavailable")).when(store).confirm(event, OwnerEventConsumer.LEARNING_CORE, NOW);
        assertThatThrownBy(() -> service.complete(delivery, "worker", NOW, NOW.plusSeconds(30))).isInstanceOf(IllegalStateException.class);
        verify(cores, never()).markAllPublished(any(), any(), any());
    }
    static OwnerEventProperties lcProperties() {
        var p = new OwnerEventProperties();
        p.setUserMergedCaptureEnabled(true);
        p.setMergeProgressCaptureEnabled(true);
        p.setMergeCompletionProfile(MergeCompletionProfile.LEARNING_CORE_ONLY);
        p.setLearningCoreUserMergedPublisherEnabled(true);
        p.setLearningCoreEndpoint(URI.create("https://learning.example.com/internal/v1/events/user-merged"));
        return p;
    }
}
