package web.tosunsaeng.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// Bootstrap 전용 임시 정책이다. 인증 기능 구현 시 공개 경로 외 요청은 반드시 보호한다.
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/actuator/health",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**"
						)
						.permitAll()
						.anyRequest().permitAll()
				)
				.formLogin(formLogin -> formLogin.disable())
				.httpBasic(httpBasic -> httpBasic.disable());

		return http.build();
	}

	@Bean
	public UserDetailsService bootstrapUserDetailsService() {
		// 빈 사용자 저장소를 제공해 Spring Boot의 임시 사용자와 생성 비밀번호 로그를 막는다.
		return new InMemoryUserDetailsManager();
	}
}
