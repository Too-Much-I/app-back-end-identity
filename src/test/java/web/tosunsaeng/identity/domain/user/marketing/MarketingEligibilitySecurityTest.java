package web.tosunsaeng.identity.domain.user.marketing;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MarketingEligibilitySecurityTest {
    static final String USER="30000000-0000-4000-8000-000000000001";
    static final RSAKey KEY=key();
    static RSAKey key(){try{return new RSAKeyGenerator(2048).keyID("fixture").algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).generate();}catch(Exception e){throw new AssertionError(e);}}
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({MarketingEligibilityConfiguration.class,MarketingEligibilityController.class})
    static class Config {
        @Bean MongoTemplate mongo(){return mock(MongoTemplate.class);}
        @Bean ConsentPolicy policy(){var p=mock(ConsentPolicy.class);when(p.getMarketingPushConsentVersion()).thenReturn("v1");return p;}
    }
    void start(boolean enabled){
        context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());
        TestPropertyValues.of("app.marketing-eligibility.enabled="+enabled,"app.marketing-eligibility.environment=test",
                "app.marketing-eligibility.lc-public-jwks="+new JWKSet(KEY.toPublicJWK()).toString()).applyTo(context);
        context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clearInvocations(context.getBean(MongoTemplate.class)); // Exclude Spring's setApplicationContext lifecycle call.
    }
    @AfterEach void close(){if(context!=null)context.close();}
    String token(String issuer,String subject,String audience,Instant now,Instant issued,Instant expires)throws Exception{
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID("fixture").build(),
                new JWTClaimsSet.Builder().issuer(issuer).subject(subject).audience(audience).issueTime(Date.from(issued))
                        .notBeforeTime(Date.from(issued)).expirationTime(Date.from(expires)).jwtID(UUID.randomUUID().toString()).build());
        jwt.sign(new RSASSASigner(KEY));return jwt.serialize();
    }
    String valid()throws Exception{Instant now=Instant.now();return token("urn:tosunsaeng:workload:learning-core:test","learning-core-service",
            "tosunsaeng-identity:marketing-push-eligibility:test",now,now,now.plusSeconds(60));}
    @Test void authenticatedExactRouteReturnsOnlyEligibilityAndStrictBody()throws Exception{
        start(true);var mongo=context.getBean(MongoTemplate.class);
        when(mongo.findOne(any(Query.class),eq(Document.class),eq("users"))).thenReturn(new Document("status","ACTIVE")
                .append("accountType","MEMBER").append("consents",new Document("marketingPushConsented",true)
                        .append("marketingPushConsentVersion","v1").append("marketingPushConsentedAt",new Date())));
        mvc.perform(post(MarketingEligibilityConfiguration.PATH).header("Authorization","Bearer "+valid()).contentType("application/json")
                .content("{\"userId\":\""+USER+"\"}")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.eligible").value(true)).andExpect(jsonPath("$.userId").value(USER)).andExpect(jsonPath("$.checkedAt").isString());
        for(String body:List.of("{}","{\"userId\":123}","{\"userId\":\""+USER+"\",\"userId\":\""+USER+"\"}",
                "{\"userId\":\""+USER+"\"}{}","{\"userId\":\""+USER+"\",\"extra\":true}"))
            mvc.perform(post(MarketingEligibilityConfiguration.PATH).header("Authorization","Bearer "+valid()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verify(mongo,times(1)).findOne(any(Query.class),eq(Document.class),eq("users"));
    }
    @Test void rejectsUnsignedUserTokensWrongPurposeEnvironmentAndExpired()throws Exception{
        start(true);Instant now=Instant.now();
        mvc.perform(post(MarketingEligibilityConfiguration.PATH)).andExpect(status().isUnauthorized());
        for(String[] claims:List.of(new String[]{"fixture-user-issuer",USER,"tosunsaeng-learning-core"},
                new String[]{"urn:tosunsaeng:workload:learning-core:test","learning-core-service","tosunsaeng-billing:promotion-eligibility:test"},
                new String[]{"urn:tosunsaeng:workload:learning-core:production","learning-core-service","tosunsaeng-identity:marketing-push-eligibility:test"}))
            mvc.perform(post(MarketingEligibilityConfiguration.PATH).header("Authorization","Bearer "+token(claims[0],claims[1],claims[2],now,now,now.plusSeconds(60))))
                    .andExpect(status().isUnauthorized());
        var decoder=MarketingEligibilityConfiguration.decoder(new JWKSet(KEY.toPublicJWK()).toString(),"test",Clock.fixed(now,ZoneOffset.UTC));
        String expired=token("urn:tosunsaeng:workload:learning-core:test","learning-core-service","tosunsaeng-identity:marketing-push-eligibility:test",now,now.minusSeconds(70),now.minusSeconds(10));
        assertThatThrownBy(()->decoder.decode(expired)).isInstanceOf(BadJwtException.class);
        verifyNoInteractions(context.getBean(MongoTemplate.class));
    }
    @Test void offDoesNotExposeEndpointEvenWithValidWorkloadToken()throws Exception{
        start(false);mvc.perform(post(MarketingEligibilityConfiguration.PATH).header("Authorization","Bearer "+valid())).andExpect(status().isUnauthorized());
        verifyNoInteractions(context.getBean(MongoTemplate.class));
    }
    @Test void refusesPrivateOrDuplicateKeysAtStartup(){
        assertThatThrownBy(()->MarketingEligibilityConfiguration.decoder(new JWKSet(KEY).toString(false),"test",Clock.systemUTC())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->MarketingEligibilityConfiguration.decoder(new JWKSet(List.of(KEY.toPublicJWK(),KEY.toPublicJWK())).toString(),"test",Clock.systemUTC())).isInstanceOf(IllegalStateException.class);
    }
}
