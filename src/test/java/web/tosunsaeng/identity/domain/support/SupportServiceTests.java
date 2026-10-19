package web.tosunsaeng.identity.domain.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.dao.DataAccessResourceFailureException;
import web.tosunsaeng.identity.global.exception.BusinessException;

class SupportServiceTests {
    final Instant now = Instant.parse("2026-10-06T00:00:00Z");
    final String key = "00000000-0000-4000-8000-000000000001";
    final SupportRequest request = new SupportRequest(SupportRequest.Category.AUTH, "테스트용 문의 내용입니다.", null, null);
    SupportStore store;
    SupportService service;
    @BeforeEach void setup() {
        store = mock(SupportStore.class);
        when(store.transaction(any())).thenAnswer(i -> ((Supplier<?>)i.getArgument(0)).get());
        service = new SupportService(store, new SupportCrypto(Base64.getEncoder().encodeToString(new byte[32])),
                new ObjectMapper(), Clock.fixed(now, ZoneOffset.UTC), new SimpleMeterRegistry());
    }
    @Test void createsReceiptAndAppliesTwoQuotas() {
        var result = service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.1", key, request);
        assertThat(result.replay()).isFalse();
        verify(store).insert(eq(result.inquiryId()), anyString(), anyString(), isNull(), eq("ANONYMOUS"), eq(request), eq(now));
        verify(store).quota(startsWith("hour:"), eq(5), any());
        verify(store).quota(startsWith("day:"), eq(10), any());
    }
    @Test void refundWithoutUserIdIsRejectedBeforeAnyStorageOrReplay() {
        var refund = new SupportRequest(SupportRequest.Category.REFUND, request.message(), null, null);
        for (String id : new String[]{null, "", " "}) {
            assertCode(() -> service.submit(new SupportService.Actor(id, "ANONYMOUS"), "127.0.0.1", key, refund),
                    "SUPPORT_REFUND_AUTH_REQUIRED");
        }
        verifyNoInteractions(store);
    }
    @Test void refundPersistsServerResolvedUserIdForMemberAndGuest() {
        var refund = new SupportRequest(SupportRequest.Category.REFUND, request.message(), null, null);
        for (String type : List.of("MEMBER", "GUEST")) {
            String id = UUID.randomUUID().toString();
            service.submit(new SupportService.Actor(id, type), "127.0.0.1", UUID.randomUUID().toString(), refund);
            verify(store).insert(anyString(), anyString(), anyString(), eq(id), eq(type), eq(refund), eq(now));
        }
    }
    @Test void replayDoesNotConsumeNewInquiryQuota() {
        doAnswer(i -> { when(store.receipt(i.getArgument(1))).thenReturn(new SupportStore.Receipt(i.getArgument(0), i.getArgument(2), now.plusSeconds(60))); return null; })
                .when(store).insert(anyString(), anyString(), anyString(), any(), anyString(), any(), any());
        var first = service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.1", key, request);
        clearInvocations(store);
        var replay = service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.2", key, request);
        assertThat(replay.inquiryId()).isEqualTo(first.inquiryId()); assertThat(replay.replay()).isTrue();
        verify(store, never()).quota(anyString(), anyInt(), any());
    }
    @Test void conflictingPayloadCannotReadExistingReceipt() {
        when(store.receipt(anyString())).thenReturn(new SupportStore.Receipt("receipt", "different", now.plusSeconds(60)));
        assertCode(() -> service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.1", key, request), "SUPPORT_INQUIRY_REQUEST_CONFLICT");
        verify(store, never()).insert(any(), any(), any(), any(), any(), any(), any());
    }
    @Test void rejectsNonV4AndUppercaseKeys() {
        for (String bad : List.of("bad", "00000000-0000-1000-8000-000000000001", "AAAAAAAA-0000-4000-8000-000000000001")) {
            assertCode(() -> service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.1", bad, request), "INVALID_SUPPORT_REQUEST_ID");
        }
        verifyNoInteractions(store);
    }
    @Test void databaseFailureRetriesOnlyThreeTimes() {
        doThrow(new DataAccessResourceFailureException("synthetic")).when(store).transaction(any());
        assertCode(() -> service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.1", key, request), "SUPPORT_INQUIRY_UNAVAILABLE");
        verify(store, times(3)).transaction(any());
    }
    @Test void quotaRejectionIsNotRetried() {
        doThrow(SupportError.SUPPORT_INQUIRY_RATE_LIMITED.exception()).when(store).quota(anyString(), anyInt(), any());
        assertCode(() -> service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.1", key, request), "SUPPORT_INQUIRY_RATE_LIMITED");
        verify(store, times(1)).transaction(any());
    }
    @Test void expiredReceiptIsReplaced() {
        when(store.receipt(anyString())).thenReturn(new SupportStore.Receipt("old", "old", now));
        assertThat(service.submit(new SupportService.Actor(null, "ANONYMOUS"), "127.0.0.1", key, request).replay()).isFalse();
        verify(store).removeExpiredReceipt(anyString(), eq(now));
    }
    @Test void requestToStringDoesNotExposeContent() {
        assertThat(request.toString()).doesNotContain(request.message());
    }
    static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode().getCode()).isEqualTo(code));
    }
}
