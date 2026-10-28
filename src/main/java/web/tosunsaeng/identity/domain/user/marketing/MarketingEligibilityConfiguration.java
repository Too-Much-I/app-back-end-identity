package web.tosunsaeng.identity.domain.user.marketing;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.SignedJWT;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

/** Independent workload authentication; ordinary user JWTs cannot read another user's consent. */
@Configuration(proxyBeanMethods=false)
public class MarketingEligibilityConfiguration {
    public static final String PATH="/workload/v1/marketing-push/eligibility";
    @Bean @Order(-1) SecurityFilterChain marketingEligibilitySecurity(HttpSecurity http,
            @Value("${app.marketing-eligibility.enabled:false}") boolean enabled,
            @Value("${app.marketing-eligibility.environment:}") String environment,
            @Value("${app.marketing-eligibility.lc-public-jwks:}") String jwks) throws Exception {
        http.securityMatcher(PATH).csrf(c->c.disable()).cors(c->c.disable()).httpBasic(c->c.disable())
                .formLogin(c->c.disable()).logout(c->c.disable()).requestCache(c->c.disable())
                .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(c->c.authenticationEntryPoint((q,r,e)->r.setStatus(401)).accessDeniedHandler((q,r,e)->r.setStatus(403)))
                .authorizeHttpRequests(c->{if(enabled)c.anyRequest().authenticated();else c.anyRequest().denyAll();});
        if(enabled) http.oauth2ResourceServer(c->c.jwt(j->j.decoder(decoder(jwks,environment,Clock.systemUTC()))));
        return http.build();
    }
    static JwtDecoder decoder(String raw, String environment, Clock clock) {
        final Map<String,RSAKey> keys=new HashMap<>();
        try {
            if (!Set.of("test","staging","production").contains(environment) || raw==null || raw.length()>65536) throw new IllegalArgumentException();
            strictJson(raw);
            for(JWK jwk:JWKSet.parse(raw).getKeys()) {
                if(!(jwk instanceof RSAKey key) || key.isPrivate() || key.size()<2048 || key.getKeyID()==null
                        || keys.putIfAbsent(key.getKeyID(),key)!=null || !JWSAlgorithm.RS256.equals(key.getAlgorithm())
                        || !KeyUse.SIGNATURE.equals(key.getKeyUse())
                        || (key.getKeyOperations()!=null && !key.getKeyOperations().equals(Set.of(KeyOperation.VERIFY)))) throw new IllegalArgumentException();
            }
            if(keys.isEmpty()) throw new IllegalArgumentException();
        } catch(Exception invalid) {throw new IllegalStateException("Invalid marketing eligibility verifier configuration");}
        return token->{
            try {
                if(token.length()>16384) throw new IllegalArgumentException();
                SignedJWT jwt=SignedJWT.parse(token); var header=jwt.getHeader(); var claims=jwt.getJWTClaimsSet();
                strictJson(jwt.getParsedParts()[0].decodeToString());
                strictJson(jwt.getParsedParts()[1].decodeToString());
                RSAKey key=keys.get(header.getKeyID()); Instant now=clock.instant();
                if(key==null || !JWSAlgorithm.RS256.equals(header.getAlgorithm()) || !JOSEObjectType.JWT.equals(header.getType())
                        || header.getJWKURL()!=null || header.getJWK()!=null || header.getCriticalParams()!=null
                        || header.getX509CertURL()!=null || header.getX509CertChain()!=null
                        || !jwt.verify(new RSASSAVerifier(key))
                        || !("urn:tosunsaeng:workload:learning-core:"+environment).equals(claims.getIssuer())
                        || !"learning-core-service".equals(claims.getSubject())
                        || !List.of("tosunsaeng-identity:marketing-push-eligibility:"+environment).equals(claims.getAudience())
                        || claims.getIssueTime()==null || claims.getNotBeforeTime()==null || claims.getExpirationTime()==null
                        || claims.getJWTID()==null || !UUID.fromString(claims.getJWTID()).toString().equals(claims.getJWTID())) throw new IllegalArgumentException();
                Instant issued=claims.getIssueTime().toInstant(), expires=claims.getExpirationTime().toInstant();
                if(!expires.isAfter(now) || issued.isAfter(now.plusSeconds(30)) || issued.isBefore(now.minusSeconds(120))
                        || !claims.getNotBeforeTime().toInstant().equals(issued) || !expires.isAfter(issued)
                        || expires.isAfter(issued.plusSeconds(60))) throw new IllegalArgumentException();
                return Jwt.withTokenValue(token).headers(h->h.putAll(header.toJSONObject()))
                        .claims(c->c.putAll(claims.getClaims())).issuedAt(issued).expiresAt(expires).build();
            } catch(Exception invalid) { throw new BadJwtException("Invalid workload token"); }
        };
    }
    private static void strictJson(String raw) throws java.io.IOException {
        var mapper=com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
        if(!mapper.readTree(raw).isObject())throw new IllegalArgumentException();
    }
}
