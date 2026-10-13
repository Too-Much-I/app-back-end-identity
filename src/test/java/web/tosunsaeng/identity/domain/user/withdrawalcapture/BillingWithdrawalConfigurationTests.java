package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.URI;
import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import web.tosunsaeng.identity.global.workload.BillingJwtConfiguration;

/** Configuration-only tests: no records claimed and no outbound HTTP. */
class BillingWithdrawalConfigurationTests {
    static String syntheticKey;
    final WithdrawalDeliveryStore store = mock(WithdrawalDeliveryStore.class);
    @BeforeAll static void key() throws Exception {
        syntheticKey = new RSAKeyGenerator(2048).keyID("fixture-publisher").generate().toJSONString();
    }
    ApplicationContextRunner enabled(String origin) {
        var properties = new WithdrawalCaptureProperties(); properties.setRequired(true); properties.setPublisherEnabled(true);
        properties.setEnvironment("test"); properties.setBillingOrigin(URI.create(origin));
        return new ApplicationContextRunner().withUserConfiguration(WithdrawalCaptureConfiguration.PublisherConfiguration.class,
                        BillingJwtConfiguration.class)
                .withBean(WithdrawalCaptureProperties.class, () -> properties)
                .withBean(WithdrawalDeliveryStore.class, () -> store)
                .withBean(Clock.class, Clock::systemUTC).withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("app.withdrawal-capture.publisher-enabled=true",
                        "app.billing-workload.private-jwk=" + syntheticKey,
                        "app.billing-workload.issuer=fixture:identity",
                        "app.billing-workload.withdrawal-audience=fixture:withdrawal");
    }
    @Test void offRequiresNeitherEvidenceNorKeyNorDatabase() {
        new ApplicationContextRunner().withUserConfiguration(WithdrawalCaptureConfiguration.PublisherConfiguration.class,
                BillingJwtConfiguration.class).run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(BillingWithdrawalPublisher.class));
    }
    @Test void onWithJwtNeedsNoClockEvidence() {
        enabled("https://billing.invalid").run(c -> {
            assertThat(c).hasNotFailed().hasSingleBean(BillingWithdrawalPublisher.class);
            assertThat(c.getBean(BillingWithdrawalPublisher.class).publishNext()).isFalse();
        });
    }
    @Test void missingKeyStillFailsClosed() {
        enabled("https://billing.invalid").withPropertyValues("app.billing-workload.private-jwk=")
                .run(c -> assertThat(c).hasFailed());
    }
    @Test void invalidOriginAndMissingAudienceStillCannotStart() {
        enabled("http://billing.invalid")
                .run(c -> assertThat(c).hasFailed());
        enabled("https://billing.invalid")
                .withPropertyValues("app.billing-workload.withdrawal-audience=")
                .run(c -> assertThat(c).hasFailed());
    }
}
