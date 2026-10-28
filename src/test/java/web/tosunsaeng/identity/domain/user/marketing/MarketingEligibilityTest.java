package web.tosunsaeng.identity.domain.user.marketing;

import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MarketingEligibilityTest {
    @Test void requiresActiveMemberAndCurrentExplicitConsent(){
        Document consent=new Document("marketingPushConsented",true).append("marketingPushConsentVersion","v1")
                .append("marketingPushConsentedAt",new Date(2000));
        Document user=new Document("status","ACTIVE").append("accountType","MEMBER").append("consents",consent);
        assertThat(MarketingEligibilityController.eligible(user,"v1")).isTrue();
        assertThat(MarketingEligibilityController.eligible(user,"v2")).isFalse();
        consent.put("marketingPushRevokedAt",new Date(3000));
        consent.put("marketingPushConsented",false);
        assertThat(MarketingEligibilityController.eligible(user,"v1")).isFalse();
        consent.put("marketingPushConsented",true);
        consent.put("marketingPushConsentedAt",new Date(3000)); // same-ms reconsent matches the public API
        assertThat(MarketingEligibilityController.eligible(user,"v1")).isTrue();
        user.put("status","WITHDRAWN");
        assertThat(MarketingEligibilityController.eligible(user,"v1")).isFalse();
        user.put("status","ACTIVE");user.put("accountType","GUEST");
        assertThat(MarketingEligibilityController.eligible(user,"v1")).isFalse();
    }
}
