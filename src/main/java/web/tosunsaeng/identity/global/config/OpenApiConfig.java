package web.tosunsaeng.identity.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
		info = @Info(
				title = "토선생 Identity API",
				description = "사용자 계정, 인증 토큰 및 로그인 세션 관리 API",
				version = "1.0.0"
		)
)
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI identityOpenApi() {
		SecurityScheme bearerScheme = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT");

		return new OpenAPI().components(
				new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme)
		);
	}
}
