package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import org.junit.jupiter.api.Test;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalTestSupport.*;

class WithdrawalMonitorTests {
    @Test void failedObservationDoesNotReplacePriorCountWithFalseZero() {
        var mongo=mock(MongoTemplate.class); var metrics=new SimpleMeterRegistry();
        var monitor=new WithdrawalMonitor(mongo,CLOCK,metrics);
        when(mongo.aggregate(any(Aggregation.class),eq("withdrawal_journal"),eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(new Document("count",2L)),new Document()))
                .thenThrow(new IllegalStateException("synthetic"));
        monitor.observe(); monitor.observe();
        assertThat(metrics.get("identity.withdrawal.billing.overdue_24h").gauge().value()).isEqualTo(2);
        assertThat(metrics.get("identity.withdrawal.monitor_failure").counter().count()).isEqualTo(1);
        assertThat(metrics.getMeters()).allSatisfy(m -> assertThat(m.getId().getTags()).isEmpty());
    }
}
