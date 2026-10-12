package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WithdrawalConfigurationTests {
    @Test void captureAloneDoesNotSatisfyPurchaseReadiness() {
        var mongo=org.mockito.Mockito.mock(org.springframework.data.mongodb.core.MongoTemplate.class);
        var evidence=org.mockito.Mockito.mock(WithdrawalCaptureConfiguration.RuntimeEvidence.class);
        var provider=new org.springframework.beans.factory.support.DefaultListableBeanFactory()
                .getBeanProvider(WithdrawalCaptureConfiguration.PurchaseActivationEvidence.class);
        var p=new WithdrawalCaptureProperties(); p.setRequired(true);
        var readiness=new WithdrawalCaptureConfiguration().withdrawalPurchaseReadiness(mongo,p,evidence,provider);
        assertThat(readiness.ready()).isFalse(); org.mockito.Mockito.verifyNoInteractions(mongo);
    }
    @Test void persistedCutoverCannotBeBypassedByTurningFlagsOff() throws Exception {
        var mongo=org.mockito.Mockito.mock(org.springframework.data.mongodb.core.MongoTemplate.class);
        org.mockito.Mockito.when(mongo.exists(org.mockito.ArgumentMatchers.any(org.springframework.data.mongodb.core.query.Query.class),
                org.mockito.ArgumentMatchers.eq(WithdrawalRecords.Stream.class))).thenReturn(true);
        var p=new WithdrawalCaptureProperties();
        var guard=new WithdrawalCaptureConfiguration().withdrawalCutoverGuard(mongo,p);
        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void offDoesNotRequireDbOrCredentialsOrRuntimeEvidence() {
        new ApplicationContextRunner().withUserConfiguration(WithdrawalCaptureConfiguration.class)
                .withInitializer(c -> c.getEnvironment().setActiveProfiles("test"))
                .run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(WithdrawalCaptureService.class)
                        .doesNotHaveBean(BillingWithdrawalPublisher.class).doesNotHaveBean(WithdrawalSourceCleanup.class));
    }
    @Test void contradictoryFlagsFailClosed() {
        var p=new WithdrawalCaptureProperties(); p.setPublisherEnabled(true);
        assertThatThrownBy(() -> p.validate(true,true)).isInstanceOf(IllegalStateException.class);
        p.setRequired(true); p.setEnvironment("test");
        assertThatThrownBy(() -> p.validate(true,true)).isInstanceOf(IllegalStateException.class);
        p.setPublisherEnabled(false); p.validate(true,true);
        assertThatThrownBy(() -> p.validate(false,true)).isInstanceOf(IllegalStateException.class);
    }
    @Test void exactIndexesRejectKeyOrderTtlSparseAndWrongPartial() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        try (var in=getClass().getResourceAsStream("/contracts/payment-lifecycle/v1/identity-indexes.json")) {
            var entries=mapper.readTree(in).get("indexes");
            assertThat(WithdrawalIndexes.SPECS).hasSize(7);
            for (int i=0;i<7;i++) {
                var spec=WithdrawalIndexes.SPECS.get(i); var fixture=entries.get(i);
                assertThat(spec.name()).isEqualTo(fixture.get("name").asText());
                assertThat(spec.keys().toJson()).isEqualTo(Document.parse(fixture.get("keys").toString()).toJson());
                var actual=new Document("key",spec.keys()).append("unique",spec.unique());
                if(spec.partial()!=null)actual.append("partialFilterExpression",spec.partial());
                WithdrawalIndexes.validate(spec,actual);
                assertThatThrownBy(() -> WithdrawalIndexes.validate(spec,new Document(actual).append("expireAfterSeconds",0)))
                        .isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> WithdrawalIndexes.validate(spec,new Document(actual).append("sparse",true)))
                        .isInstanceOf(IllegalStateException.class);
            }
        }
        var spec=WithdrawalIndexes.SPECS.get(0);
        assertThatThrownBy(() -> WithdrawalIndexes.validate(spec,new Document("key",new Document("streamId",1).append("environment",1)).append("unique",true)))
                .isInstanceOf(IllegalStateException.class);
    }
}
