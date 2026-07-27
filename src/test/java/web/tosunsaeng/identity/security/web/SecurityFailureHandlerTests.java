package web.tosunsaeng.identity.security.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityFailureHandlerTests {

	private static final String TEST_ONLY_BEARER_VALUE = "handler-test-bearer-value";
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void authenticationEntryPointWritesSafeUtf8BaseResponse() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		BaseResponseAuthenticationEntryPoint entryPoint = new BaseResponseAuthenticationEntryPoint(
				objectMapper
		);

		entryPoint.commence(
				new MockHttpServletRequest(),
				response,
				new BadCredentialsException("internal " + TEST_ONLY_BEARER_VALUE)
		);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
		assertThat(response.getContentAsString())
				.isEqualTo("{\"isSuccess\":false,\"code\":\"COMMON_UNAUTHORIZED\","
						+ "\"message\":\"인증이 필요합니다.\",\"result\":null}")
				.doesNotContain(TEST_ONLY_BEARER_VALUE, "BadCredentialsException", "stackTrace");
	}

	@Test
	void accessDeniedHandlerWritesSafeUtf8BaseResponse() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		BaseResponseAccessDeniedHandler handler = new BaseResponseAccessDeniedHandler(objectMapper);

		handler.handle(
				new MockHttpServletRequest(),
				response,
				new AccessDeniedException("internal " + TEST_ONLY_BEARER_VALUE)
		);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
		assertThat(response.getContentAsString())
				.isEqualTo("{\"isSuccess\":false,\"code\":\"COMMON_FORBIDDEN\","
						+ "\"message\":\"접근 권한이 없습니다.\",\"result\":null}")
				.doesNotContain(TEST_ONLY_BEARER_VALUE, "AccessDeniedException", "stackTrace");
	}
}
