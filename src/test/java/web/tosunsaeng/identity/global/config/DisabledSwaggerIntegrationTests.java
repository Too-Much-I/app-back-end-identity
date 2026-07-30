package web.tosunsaeng.identity.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.TestRsaKeyConfiguration;

@SpringBootTest(properties = {
		"springdoc.api-docs.enabled=false",
		"springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRsaKeyConfiguration.class)
class DisabledSwaggerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private RefreshSessionRepository refreshSessionRepository;

	@ParameterizedTest
	@ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
	void disabledDocumentationEndpointsReturnSafeNotFoundResponse(String path) throws Exception {
		MvcResult result = mockMvc.perform(get(path))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain(
				"NoResourceFoundException",
				"stackTrace"
		);
	}
}
