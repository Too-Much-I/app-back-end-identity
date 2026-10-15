package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static web.tosunsaeng.identity.domain.auth.accountrecovery.PhoneOnlyCleanupPolicy.*;

class PhoneOnlyCleanupPolicyTests {
    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");
    private static final Instant OLD = NOW.minusSeconds(3600);
    private static final ProductionState CLEAR = new ProductionState(true, false, false, false, false);
    private final PhoneOnlyCleanupPolicy policy = new PhoneOnlyCleanupPolicy();

    private Snapshot snapshot(Set<String> providers, Instant created, Instant signedIn, Instant refresh, Instant recovery) {
        return new Snapshot(providers, true, false, false, false, created, signedIn, refresh, recovery);
    }
    private Snapshot oldPhone() { return snapshot(Set.of("phone"), OLD, OLD, null, null); }

    @Test void oldUnboundPhoneOnlyIsCandidateNotDeletionPermission() {
        assertThat(policy.evaluate(oldPhone(), CLEAR, NOW)).isEqualTo(Decision.CANDIDATE);
        assertThat(SCAN_INTERVAL).isEqualTo(Duration.ofMinutes(5));
    }
    @ParameterizedTest @ValueSource(strings = {"google.com", "apple.com", "oidc.kakao", "password", "unknown"})
    void anyOtherProviderExcludesBothSnsOnlyAndSnsWithPhone(String provider) {
        assertThat(policy.evaluate(snapshot(Set.of(provider), OLD, OLD, null, null), CLEAR, NOW))
                .isEqualTo(Decision.NOT_PHONE_ONLY);
        assertThat(policy.evaluate(snapshot(Set.of("phone", provider), OLD, OLD, null, null), CLEAR, NOW))
                .isEqualTo(Decision.NOT_PHONE_ONLY);
    }
    @Test void exactThirtyMinutesBoundary() {
        assertThat(policy.evaluate(snapshot(Set.of("phone"), OLD, NOW.minusSeconds(1800), null, null), CLEAR, NOW))
                .isEqualTo(Decision.CANDIDATE);
        assertThat(policy.evaluate(snapshot(Set.of("phone"), OLD, NOW.minusSeconds(1800).plusNanos(1), null, null), CLEAR, NOW))
                .isEqualTo(Decision.RECENT);
    }
    @Test void eachObservedActivityExtendsProtection() {
        Instant recent = NOW.minusSeconds(60);
        for (Snapshot s : new Snapshot[] {
                snapshot(Set.of("phone"), recent, OLD, null, null),
                snapshot(Set.of("phone"), OLD, recent, null, null),
                snapshot(Set.of("phone"), OLD, OLD, recent, null),
                snapshot(Set.of("phone"), OLD, OLD, null, recent) }) {
            assertThat(policy.evaluate(s, CLEAR, NOW)).isEqualTo(Decision.RECENT);
        }
    }
    @Test void everyProductionProtectionExcludesTarget() {
        for (ProductionState state : new ProductionState[] {
                new ProductionState(true, true, false, false, false),
                new ProductionState(true, false, true, false, false),
                new ProductionState(true, false, false, true, false),
                new ProductionState(true, false, false, false, true) }) {
            assertThat(policy.evaluate(oldPhone(), state, NOW)).isEqualTo(Decision.PROTECTED);
        }
    }
    @Test void missingOrFailedInformationNeverSelects() {
        assertThat(policy.evaluate(null, CLEAR, NOW)).isEqualTo(Decision.UNKNOWN);
        assertThat(policy.evaluate(oldPhone(), null, NOW)).isEqualTo(Decision.UNKNOWN);
        assertThat(policy.evaluate(oldPhone(), new ProductionState(false, false, false, false, false), NOW))
                .isEqualTo(Decision.UNKNOWN);
        assertThat(policy.evaluate(snapshot(null, OLD, OLD, null, null), CLEAR, NOW)).isEqualTo(Decision.UNKNOWN);
        assertThat(policy.evaluate(snapshot(Set.of("phone"), null, OLD, null, null), CLEAR, NOW)).isEqualTo(Decision.UNKNOWN);
        assertThat(policy.evaluate(snapshot(Set.of("phone"), OLD, null, null, null), CLEAR, NOW)).isEqualTo(Decision.UNKNOWN);
    }
    @Test void futureActivityIsUnknown() {
        assertThat(policy.evaluate(snapshot(Set.of("phone"), OLD, OLD, NOW.plusSeconds(1), null), CLEAR, NOW))
                .isEqualTo(Decision.UNKNOWN);
    }
    @Test void disabledEmailAndClaimsAccountsAreNotCandidates() {
        assertThat(policy.evaluate(new Snapshot(Set.of("phone"), true, false, false, true, OLD, OLD, null, null), CLEAR, NOW))
                .isEqualTo(Decision.PROTECTED);
        assertThat(policy.evaluate(new Snapshot(Set.of("phone"), true, true, false, false, OLD, OLD, null, null), CLEAR, NOW))
                .isEqualTo(Decision.NOT_PHONE_ONLY);
        assertThat(policy.evaluate(new Snapshot(Set.of("phone"), true, false, true, false, OLD, OLD, null, null), CLEAR, NOW))
                .isEqualTo(Decision.NOT_PHONE_ONLY);
        assertThat(policy.evaluate(new Snapshot(Set.of("phone"), false, false, false, false, OLD, OLD, null, null), CLEAR, NOW))
                .isEqualTo(Decision.NOT_PHONE_ONLY);
        assertThat(policy.evaluate(snapshot(Set.of(), OLD, OLD, null, null), CLEAR, NOW)).isEqualTo(Decision.NOT_PHONE_ONLY);
    }
}
