package web.tosunsaeng.identity.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import web.tosunsaeng.identity.global.security.web.BaseResponseAccessDeniedHandler;
import web.tosunsaeng.identity.global.security.web.BaseResponseAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtDecoder jwtDecoder,
			BaseResponseAuthenticationEntryPoint authenticationEntryPoint,
			BaseResponseAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						// 인증 진입점과 문서, 상태 확인, JWKS만 공개한다.
						.requestMatchers(HttpMethod.POST,
								"/api/v1/auth/check-email",
								"/api/v1/auth/signup",
								"/api/v1/auth/guest",
								"/api/v1/auth/firebase/exchange",
								"/api/v1/auth/firebase/signup",
								"/api/v1/auth/login",
								"/api/v1/auth/reissue",
								"/api/v1/auth/logout"
						)
						.permitAll()
						.requestMatchers(HttpMethod.GET,
								"/.well-known/jwks.json",
								"/actuator/health",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs",
								"/v3/api-docs/**"
						)
						.permitAll()
						// 명시적으로 공개하지 않은 모든 요청은 JWT 인증을 요구한다.
						.anyRequest().authenticated()
				)
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
				)
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.decoder(jwtDecoder))
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
				)
				.formLogin(formLogin -> formLogin.disable())
				.httpBasic(httpBasic -> httpBasic.disable());

		return http.build();
	}

	@Bean
	public BaseResponseAuthenticationEntryPoint authenticationEntryPoint(
			ObjectMapper objectMapper
	) {
		return new BaseResponseAuthenticationEntryPoint(objectMapper);
	}

	@Bean
	public BaseResponseAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
		return new BaseResponseAccessDeniedHandler(objectMapper);
	}

	@Bean
	public UserDetailsService bootstrapUserDetailsService() {
		// 빈 사용자 저장소를 제공해 Spring Boot의 임시 사용자와 생성 비밀번호 로그를 막는다.
		return new InMemoryUserDetailsManager();
	}
}
