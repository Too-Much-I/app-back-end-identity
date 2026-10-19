package web.tosunsaeng.identity.domain.appversion;

import org.springframework.http.HttpStatus;
import web.tosunsaeng.identity.global.exception.ErrorCode;

public enum AppVersionErrorStatus implements ErrorCode {
	APP_VERSION_UNAVAILABLE;

	@Override
	public HttpStatus getHttpStatus() { return HttpStatus.SERVICE_UNAVAILABLE; }

	@Override
	public String getCode() { return name(); }

	@Override
	public String getMessage() { return "앱 버전 정보가 아직 설정되지 않았습니다."; }
}
