package web.tosunsaeng.identity.domain.appversion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import web.tosunsaeng.identity.global.response.BaseResponse;

@Tag(name = "App Version", description = "인증 없는 최신 출시 앱 버전 조회")
@RestController
@RequiredArgsConstructor
public class AppVersionController {
	private final AppVersionService service;

	@Operation(summary = "플랫폼별 최신 앱 버전 조회", description = "토큰 없이 호출합니다. 설치 버전 비교·업데이트 정책은 제공하지 않습니다.")
	@ApiResponse(responseCode = "200", description = "최신 출시 버전")
	@ApiResponse(responseCode = "400", description = "INVALID_REQUEST: 플랫폼 누락 또는 android/ios 이외 값")
	@ApiResponse(responseCode = "503", description = "APP_VERSION_UNAVAILABLE: 요청 플랫폼의 버전 미설정")
	@GetMapping(value = "/api/v1/app/version", produces = "application/json")
	public BaseResponse<AppVersionResponse> getLatest(
			@Parameter(required = true, schema = @Schema(allowableValues = {"android", "ios"}))
			@RequestParam(name = "platform", required = false) String platform,
			HttpServletResponse response
	) {
		// 미설정/입력 오류도 캐시하지 않아 설정 후 이전 오류가 남지 않도록 한다.
		response.setHeader("Cache-Control", "no-store");
		return BaseResponse.success(service.getLatest(platform));
	}
}
