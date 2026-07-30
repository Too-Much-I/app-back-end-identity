package web.tosunsaeng.identity.global.security.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import web.tosunsaeng.identity.global.exception.CommonErrorStatus;
import web.tosunsaeng.identity.global.response.BaseResponse;

public final class BaseResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public BaseResponseAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authenticationException
	) throws IOException {
		response.setStatus(CommonErrorStatus.UNAUTHORIZED.getHttpStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(
				response.getWriter(),
				BaseResponse.failure(CommonErrorStatus.UNAUTHORIZED)
		);
	}
}
