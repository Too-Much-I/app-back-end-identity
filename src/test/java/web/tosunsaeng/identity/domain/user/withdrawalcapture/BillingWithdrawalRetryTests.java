package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.util.List;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalTestSupport.*;

class BillingWithdrawalRetryTests {
    final BillingWithdrawalRetry policy=new BillingWithdrawalRetry(() -> .5);
    @ParameterizedTest @ValueSource(ints={200,201,202,205,299,300,301,307,400,404,405,409,413,422,499})
    void permanentOrWrongAck(int code) { assertThat(policy.classify(code,List.of(),1,NOW,true).state()).isEqualTo(State.DEAD_LETTER); }
    @ParameterizedTest @ValueSource(ints={408,425,429,500,503,599,0,100,199,600})
    void retryable(int code) {
        var d=policy.classify(code,List.of(),1,NOW,true);
        assertThat(d.state()).isEqualTo(State.PENDING); assertThat(d.nextAttemptAt()).isAfter(NOW);
    }
    @ParameterizedTest @ValueSource(ints={401,403})
    void authNeverBusyRetries(int code) { assertThat(policy.classify(code,List.of(),1,NOW,true).state()).isEqualTo(State.BLOCKED_AUTH); }
    @Test void twentiethSuccessAndFailure() {
        assertThat(policy.classify(204,List.of(),20,NOW,true).state()).isEqualTo(State.DELIVERED);
        assertThat(policy.classify(503,List.of("3600"),20,NOW,true).state()).isEqualTo(State.DEAD_LETTER);
        assertThat(policy.classify(503,List.of("3600"),20,NOW,true).nextAttemptAt()).isEqualTo(NOW.plusSeconds(3600));
    }
    @Test void datesAndLongDelayAreNotTruncated() {
        String date=DateTimeFormatter.RFC_1123_DATE_TIME.format(NOW.plusSeconds(600).atZone(ZoneOffset.UTC));
        assertThat(policy.classify(429,List.of(date),1,NOW,true).nextAttemptAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(policy.classify(503,List.of("86400"),1,NOW,true).state()).isEqualTo(State.PENDING);
        var longDelay=policy.classify(503,List.of("86401"),1,NOW,true);
        assertThat(longDelay.state()).isEqualTo(State.BLOCKED_RETRY_AFTER);
        assertThat(longDelay.nextAttemptAt()).isEqualTo(NOW.plusSeconds(86401));
    }
    @ParameterizedTest @ValueSource(strings={"-1","+1","999999999999999999999999999999","bad","1.5"})
    void malformedFallsBackWithoutRawValue(String value) {
        var d=policy.classify(429,List.of(value),1,NOW,true);
        assertThat(d.state()).isEqualTo(State.PENDING); assertThat(d.reason()).isEqualTo("INVALID_RETRY_AFTER");
    }
    @Test void ambiguityClockAndPastDate() {
        assertThat(policy.classify(429,List.of("1","2"),1,NOW,true).state()).isEqualTo(State.BLOCKED_HEADER);
        assertThat(policy.classify(503,List.of(),1,NOW,false).state()).isEqualTo(State.BLOCKED_CLOCK);
        assertThat(policy.classify(429,List.of("0"),1,NOW,true).nextAttemptAt()).isAfter(NOW);
        String past=DateTimeFormatter.RFC_1123_DATE_TIME.format(NOW.minusSeconds(1).atZone(ZoneOffset.UTC));
        assertThat(policy.classify(503,List.of(past),1,NOW,true).nextAttemptAt()).isAfter(NOW);
    }
    @Test void jitterIsBoundedAndNetworkBudgetStops() {
        assertThat(policy.network(19,NOW).nextAttemptAt()).isBeforeOrEqualTo(NOW.plusSeconds(3600));
        assertThat(policy.network(20,NOW).state()).isEqualTo(State.DEAD_LETTER);
        assertThat(new BillingWithdrawalRetry(() -> 0).network(1,NOW).nextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
    }
}
