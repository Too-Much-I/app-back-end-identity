package web.tosunsaeng.identity.global.security.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import web.tosunsaeng.identity.global.exception.CommonErrorStatus;
import web.tosunsaeng.identity.global.observability.RequestLogContext;
import web.tosunsaeng.identity.global.response.BaseResponse;

public final class BaseResponseAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public BaseResponseAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException
	) throws IOException {
		RequestLogContext.recordErrorCode(request, CommonErrorStatus.FORBIDDEN.getCode());
		response.setStatus(CommonErrorStatus.FORBIDDEN.getHttpStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(
				response.getWriter(),
				BaseResponse.failure(CommonErrorStatus.FORBIDDEN)
		);
	}
}
