package web.tosunsaeng.identity.domain.auth.mergeprogress;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.aop.support.AopUtils;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.*;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.*;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.*;
import static web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressTests.*;

/** Exercises real Spring transaction advice; replica-set rollback of actual writes remains an E2E gate. */
class MergeProgressTransactionTests {
    @Test void confirmationFailureRollsBackPublisherTransactionAndCaptureIsProxied() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var service = context.getBean(OwnerEventPublishTransactionService.class);
            assertThat(AopUtils.isAopProxy(service)).isTrue();
            assertThat(AopUtils.isAopProxy(context.getBean(OwnerEventCaptureService.class))).isTrue();
            var event = core(MergeCompletionProfile.LEARNING_CORE_ONLY);
            var delivery = OwnerEventDelivery.create(event.getEventId(), OwnerEventConsumer.LEARNING_CORE, 1, NOW);
            var deliveries = context.getBean(OwnerEventDeliveryRepository.class);
            var states = context.getBean(OwnerEventConsumerStateRepository.class);
            var cores = context.getBean(OwnerEventCoreRepository.class);
            var progress = context.getBean(MergeProgressStore.class);
            var manager = context.getBean(PlatformTransactionManager.class);
            when(deliveries.markPublished(any(), any(), any(), any())).thenReturn(true);
            when(states.advancePublished(any(), anyLong(), any())).thenReturn(true);
            when(cores.findById(event.getEventId())).thenReturn(Optional.of(event));
            doThrow(new IllegalStateException("progress write failed")).when(progress).confirm(event, OwnerEventConsumer.LEARNING_CORE, NOW);
            assertThatThrownBy(() -> service.complete(delivery, "worker", NOW, NOW.plus(Duration.ofDays(30))))
                    .isInstanceOf(IllegalStateException.class);
            verify(manager).rollback(any());
            verify(manager, never()).commit(any());
        }
    }
    @Configuration(proxyBeanMethods=false)
    @EnableTransactionManagement
    static class Config {
        @Bean("mongoTransactionManager") PlatformTransactionManager manager() {
            var manager = mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            return manager;
        }
        @Bean OwnerEventCoreRepository cores() { return mock(OwnerEventCoreRepository.class); }
        @Bean OwnerEventDeliveryRepository deliveries() { return mock(OwnerEventDeliveryRepository.class); }
        @Bean OwnerEventConsumerStateRepository states() { return mock(OwnerEventConsumerStateRepository.class); }
        @Bean MergeProgressStore progress() { return mock(MergeProgressStore.class); }
        @Bean OwnerEventCaptureService capture(OwnerEventCoreRepository cores, OwnerEventDeliveryRepository deliveries, OwnerEventConsumerStateRepository states) {
            return new OwnerEventCaptureService(cores, deliveries, states);
        }
        @Bean OwnerEventPublishTransactionService publisher(OwnerEventCoreRepository cores, OwnerEventDeliveryRepository deliveries,
                OwnerEventConsumerStateRepository states, MergeProgressStore progress) {
            var service = new OwnerEventPublishTransactionService(deliveries, states, cores, mock(PhoneRejoinLineageRepository.class));
            service.configureProgress(progress);
            return service;
        }
    }
}
