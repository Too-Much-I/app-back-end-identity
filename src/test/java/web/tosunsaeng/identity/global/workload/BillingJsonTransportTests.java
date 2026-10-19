package web.tosunsaeng.identity.global.workload;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillingJsonTransportTests {
    @Test void retainsSingleIntegerRetryAfterContract() {
        assertThat(BillingJsonTransport.parseRetryAfter(List.of("1"))).isEqualTo(1);
        assertThat(BillingJsonTransport.parseRetryAfter(List.of("300"))).isEqualTo(300);
        for (var values : List.of(List.<String>of(), List.of("0"), List.of("301"), List.of("1000"),
                List.of(" 1"), List.of("+1"), List.of("1", "2"), List.of("Wed, 21 Oct 2015 07:28:00 GMT")))
            assertThat(BillingJsonTransport.parseRetryAfter(values)).isNull();
        assertThat(BillingJsonTransport.parseRetryAfter(null)).isNull();
    }
    @Test void detailedResultPreservesDateAndDuplicateHeadersForWithdrawalClassifier() {
        var values = List.of("Wed, 21 Oct 2015 07:28:00 GMT", "30");
        BillingJsonTransport transport = (route, body) -> new BillingJsonTransport.DetailedResult(429, values);
        assertThat(transport.postDetailed("unused", new byte[0]).retryAfter()).isEqualTo(values);
        assertThat(transport.post("unused", new byte[0])).isEqualTo(new WorkloadDeliveryResult(429, null));
    }
}
