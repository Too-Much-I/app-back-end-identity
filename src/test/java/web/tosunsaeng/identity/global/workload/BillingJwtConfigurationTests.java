package web.tosunsaeng.identity.global.workload;

import static org.assertj.core.api.Assertions.*;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class BillingJwtConfigurationTests {
    static RSAKey key;
    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(BillingJwtConfiguration.class);
    @BeforeAll static void keys() throws Exception { key = new RSAKeyGenerator(2048).keyID("fixture-config").generate(); }
    @ParameterizedTest @CsvSource({
        "app.withdrawal-capture.publisher-enabled,withdrawal-audience",
        "app.phone-eligibility-publisher.enabled,trial-audience",
        "app.owner-event.billing-user-merged-publisher-enabled,merge-audience",
        "app.owner-event.billing-trial-rebind-publisher-enabled,trial-owner-audience"
    })
    void eachPublisherRequiresOnlyItsActiveAudience(String flag, String audience) {
        runner.withPropertyValues(flag + "=true").run(c -> assertThat(c).hasFailed());
        var active = runner.withPropertyValues(flag + "=true", "app.billing-workload.private-jwk=" + key.toJSONString(),
                "app.billing-workload.issuer=fixture:issuer");
        active.run(c -> assertThat(c).hasFailed());
        active.withPropertyValues("app.billing-workload." + audience + "=fixture:audience")
                .run(c -> { assertThat(c).hasNotFailed(); assertThat(c).hasSingleBean(BillingJwtJsonTransport.Factory.class); });
    }
    @Test void allOffAndLcOnlyNeedNoSigningKey() {
        runner.withPropertyValues("app.owner-event.learning-core-user-merged-publisher-enabled=true",
                "app.billing-workload.private-jwk=deliberately-invalid-unused")
                .run(c -> { assertThat(c).hasNotFailed(); assertThat(c).doesNotHaveBean(BillingJwtJsonTransport.Factory.class); });
    }
    @Test void activeDamagedKeyFailsWithSanitizedError() {
        runner.withPropertyValues("app.withdrawal-capture.publisher-enabled=true", "app.billing-workload.private-jwk=deliberately-invalid",
                "app.billing-workload.issuer=fixture:issuer", "app.billing-workload.withdrawal-audience=fixture:audience")
                .run(c -> {
                    assertThat(c).hasFailed();
                    assertThat(c.getStartupFailure()).hasRootCauseMessage("Invalid Billing workload signing configuration.");
                });
    }
    @ParameterizedTest @ValueSource(strings = {
        "app.billing-workload.jwt-enabled", "app.withdrawal-capture.region",
        "app.phone-eligibility-publisher.region", "app.owner-event.billing-region",
        "BILLING_WORKLOAD_JWT_ENABLED", "BILLING_WITHDRAWAL_REGION",
        "PHONE_ELIGIBILITY_PUBLISHER_REGION", "OWNER_EVENT_BILLING_REGION"
    })
    void stalePropertyFailsEvenWhenOffAndValueIsEmpty(String property) {
        for (String value : new String[]{"", "false", "synthetic"})
            runner.withPropertyValues(property + "=" + value).run(c -> {
                assertThat(c).hasFailed(); assertThat(c.getStartupFailure()).hasMessageContaining("Remove retired Billing setting:");
            });
    }
    @Test void relaxedSystemEnvironmentNameIsRejected() {
        runner.withInitializer(c -> c.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource("fixture",
                Map.of("APP_BILLING_WORKLOAD_JWT_ENABLED", "false")))).run(c -> assertThat(c).hasFailed());
    }
    @Test void enablingSecondPublisherRequiresSecondAudience() {
        runner.withPropertyValues("app.withdrawal-capture.publisher-enabled=true", "app.phone-eligibility-publisher.enabled=true",
                "app.billing-workload.private-jwk=" + key.toJSONString(), "app.billing-workload.issuer=fixture:issuer",
                "app.billing-workload.withdrawal-audience=fixture:withdrawal")
                .run(c -> assertThat(c).hasFailed());
    }
}
