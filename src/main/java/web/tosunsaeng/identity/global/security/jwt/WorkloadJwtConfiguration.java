package web.tosunsaeng.identity.global.security.jwt;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredentialProvider;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WorkloadJwtProperties.class)
public class WorkloadJwtConfiguration {

	@Bean
	@ConditionalOnProperty(
			prefix = "app.workload-jwt",
			name = "enabled",
			havingValue = "true"
	)
	WorkloadIdentityCredentialProvider workloadIdentityCredentialProvider(
			JwtEncoder jwtEncoder,
			JwtProperties jwtProperties,
			WorkloadJwtProperties properties,
			Clock clock
	) {
		properties.validate();
		return new JwtWorkloadIdentityCredentialProvider(
				jwtEncoder,
				jwtProperties,
				properties,
				clock
		);
	}
}
