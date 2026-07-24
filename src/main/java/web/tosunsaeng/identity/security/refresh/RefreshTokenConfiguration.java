package web.tosunsaeng.identity.security.refresh;

import java.security.SecureRandom;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class RefreshTokenConfiguration {

	@Bean
	public SecureRandom refreshTokenSecureRandom() {
		return new SecureRandom();
	}
}
