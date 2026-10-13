package web.tosunsaeng.identity.global.workload;

import java.time.Clock;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
public class BillingJwtConfiguration {
    /** Reject stale deployment settings even when all publishers are OFF; never log their values. */
    @Bean static BeanFactoryPostProcessor rejectRetiredBillingAuthenticationSettings(Environment environment) {
        return factory -> {
            String[][] retired = {
                {"app.billing-workload.jwt-enabled", "BILLING_WORKLOAD_JWT_ENABLED"},
                {"app.withdrawal-capture.region", "BILLING_WITHDRAWAL_REGION"},
                {"app.phone-eligibility-publisher.region", "PHONE_ELIGIBILITY_PUBLISHER_REGION"},
                {"app.owner-event.billing-region", "OWNER_EVENT_BILLING_REGION"}
            };
            for (String[] names : retired) for (String name : names)
                if (environment.containsProperty(name)) throw new IllegalStateException("Remove retired Billing setting: " + name);
        };
    }
    @Conditional(BillingPublishersEnabled.class)
    @Bean BillingJwtJsonTransport.Factory billingJwtTransportFactory(
            Environment environment) {
        var audiences = new LinkedHashMap<String, String>();
        add(environment, audiences, "app.withdrawal-capture.publisher-enabled", "/internal/v1/payments/accounts/withdrawal/events", "withdrawal-audience");
        add(environment, audiences, "app.phone-eligibility-publisher.enabled", "/internal/v1/eligibility/trial/events", "trial-audience");
        add(environment, audiences, "app.owner-event.billing-user-merged-publisher-enabled", "/internal/v1/owners/merge/events", "merge-audience");
        add(environment, audiences, "app.owner-event.billing-trial-rebind-publisher-enabled", "/internal/v1/eligibility/trial/owner/events", "trial-owner-audience");
        return new BillingJwtJsonTransport.Factory(environment.getProperty("app.billing-workload.private-jwk", ""),
                environment.getProperty("app.billing-workload.issuer", ""), audiences, Clock.systemUTC());
    }
    private static void add(Environment environment, java.util.Map<String, String> audiences, String flag, String route, String setting) {
        if (environment.getProperty(flag, Boolean.class, false))
            audiences.put(route, environment.getProperty("app.billing-workload." + setting, ""));
    }
    static class BillingPublishersEnabled extends AnyNestedCondition {
        BillingPublishersEnabled() { super(ConfigurationPhase.REGISTER_BEAN); }
        @ConditionalOnProperty(name = "app.withdrawal-capture.publisher-enabled", havingValue = "true")
        static class Withdrawal { }
        @ConditionalOnProperty(name = "app.phone-eligibility-publisher.enabled", havingValue = "true")
        static class Trial { }
        @ConditionalOnProperty(name = "app.owner-event.billing-user-merged-publisher-enabled", havingValue = "true")
        static class Merge { }
        @ConditionalOnProperty(name = "app.owner-event.billing-trial-rebind-publisher-enabled", havingValue = "true")
        static class TrialOwner { }
    }
}
