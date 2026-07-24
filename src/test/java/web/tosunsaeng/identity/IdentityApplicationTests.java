package web.tosunsaeng.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.common.exception.CommonErrorStatus;
import web.tosunsaeng.identity.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IdentityApplicationTests.TestEndpointConfiguration.class)
class IdentityApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void actuatorHealthIsAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void openApiDocsAreAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.openapi").exists());
	}

	@Test
	void bootstrapPolicyDoesNotForceLoginOrBasicAuthentication() throws Exception {
		mockMvc.perform(get("/__test/open"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value("open"));
	}

	@Test
	void validationErrorUsesBaseResponseAndMasksSensitiveRejectedValue() throws Exception {
		mockMvc.perform(post("/__test/validation")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
				.andExpect(jsonPath("$.result[0].field").value("password"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()))
				.andExpect(jsonPath("$.result[0].reason").isNotEmpty());
	}

	@Test
	void businessExceptionUsesConfiguredErrorCode() throws Exception {
		mockMvc.perform(get("/__test/business-error"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void unexpectedExceptionDoesNotExposeInternalDetailsOrStackTrace() throws Exception {
		MvcResult result = mockMvc.perform(get("/__test/unexpected-error"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		assertThat(responseBody)
				.doesNotContain("sensitive internal detail")
				.doesNotContain("IllegalStateException")
				.doesNotContain("stackTrace");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestEndpointConfiguration {

		@Bean
		TestEndpointController testEndpointController() {
			return new TestEndpointController();
		}
	}

	@RestController
	static class TestEndpointController {

		@GetMapping("/__test/open")
		Map<String, String> open() {
			return Map.of("status", "open");
		}

		@PostMapping("/__test/validation")
		void validate(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/__test/business-error")
		void businessError() {
			throw new BusinessException(CommonErrorStatus.NOT_FOUND);
		}

		@GetMapping("/__test/unexpected-error")
		void unexpectedError() {
			throw new IllegalStateException("sensitive internal detail");
		}
	}

	record ValidationRequest(@NotBlank String password) {
	}
}
