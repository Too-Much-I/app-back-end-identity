package web.tosunsaeng.identity.domain.user.marketing;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;

@RestController
@ConditionalOnProperty(name="app.marketing-eligibility.enabled",havingValue="true")
public class MarketingEligibilityController {
    private final MongoTemplate mongo;
    private final ConsentPolicy policy;
    private final ObjectMapper json=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public MarketingEligibilityController(MongoTemplate mongo,ConsentPolicy policy){this.mongo=mongo;this.policy=policy;}
    public record Result(String userId,boolean eligible,String checkedAt){
        @Override public String toString(){return "MarketingEligibility[REDACTED]";}
    }
    @PostMapping(value=MarketingEligibilityConfiguration.PATH,consumes="application/json")
    public ResponseEntity<Result> query(HttpServletRequest request) {
        String user;
        try {
            if(request.getQueryString()!=null || request.getContentLengthLong()>16384
                    || Collections.list(request.getHeaders("Authorization")).size()!=1
                    || request.getHeader("Content-Encoding")!=null) throw new IllegalArgumentException();
            byte[] body=request.getInputStream().readNBytes(16385);
            if(body.length>16384) throw new IllegalArgumentException();
            JsonNode node=json.readTree(body);
            if(node==null || !node.isObject() || node.size()!=1 || !node.path("userId").isTextual())throw new IllegalArgumentException();
            user=node.get("userId").textValue();
            if(!UUID.fromString(user).toString().equals(user))throw new IllegalArgumentException();
        } catch(Exception invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid request");}
        Query query=Query.query(Criteria.where("_id").is(user));
        query.fields().include("status","accountType","consents"); query.maxTime(Duration.ofSeconds(2));
        Document row=mongo.findOne(query,Document.class,"users");
        return ResponseEntity.ok().header("Cache-Control","no-store")
                .body(new Result(user,eligible(row,policy.getMarketingPushConsentVersion()),Instant.now().toString()));
    }
    static boolean eligible(Document user,String version){
        if(user==null || !"ACTIVE".equals(user.getString("status")) || !"MEMBER".equals(user.getString("accountType")))return false;
        Document consent=user.get("consents",Document.class);
        return consent!=null && Boolean.TRUE.equals(consent.getBoolean("marketingPushConsented"))
                && version.equals(consent.getString("marketingPushConsentVersion")) && consent.getDate("marketingPushConsentedAt")!=null;
    }
}
