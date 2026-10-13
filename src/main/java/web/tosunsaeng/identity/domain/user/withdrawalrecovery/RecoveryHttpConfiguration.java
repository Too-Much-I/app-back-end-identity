package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration(proxyBeanMethods = false)
public class RecoveryHttpConfiguration {
    /** Always reserve these paths ahead of the public user-JWT chain, including while OFF. */
    @Bean @Order(1)
    SecurityFilterChain recoverySecurityFilterChain(HttpSecurity http,
            @Value("${app.withdrawal-recovery.http-enabled:false}") boolean enabled,
            ObjectProvider<RecoveryIngressVerifier> verifier,
            ObjectProvider<RecoverySnapshotService> snapshots,
            ObjectProvider<RecoveryFeedService> feed) throws Exception {
        http.securityMatcher(RecoveryProtocol.PREFIX, RecoveryProtocol.PREFIX + "/**")
                .csrf(c -> c.disable()).cors(c -> c.disable())
                .requestCache(c -> c.disable())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(c -> c.anyRequest().denyAll())
                .exceptionHandling(c -> c.authenticationEntryPoint((q, r, e) -> { r.setHeader("Cache-Control", "no-store"); r.setStatus(403); })
                        .accessDeniedHandler((q, r, e) -> { r.setHeader("Cache-Control", "no-store"); r.setStatus(403); }))
                .formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable());
        if (enabled) {
            var auth = verifier.getIfAvailable(); var snapshot = snapshots.getIfAvailable(); var page = feed.getIfAvailable();
            if (auth == null || snapshot == null || page == null)
                throw new IllegalStateException("Recovery HTTP requires explicit trusted ingress and runtime services.");
            http.addFilterBefore(new RecoveryHttpFilter(auth, snapshot, page), AuthorizationFilter.class);
        }
        return http.build();
    }
}
