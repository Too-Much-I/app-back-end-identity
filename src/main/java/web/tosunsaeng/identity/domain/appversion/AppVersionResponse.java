package web.tosunsaeng.identity.domain.appversion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "플랫폼별 최소 지원·최신 출시 앱 버전. 설치 버전 비교와 업데이트 UI는 앱에서 수행합니다.")
public record AppVersionResponse(
		@Schema(allowableValues = {"android", "ios"}, example = "android") String platform,
		@Schema(description = "최신 출시 버전. 이 값 미만이며 최소 지원 버전 이상이면 선택적 권장 업데이트", example = "1.2.0") String latestVersion,
		@Schema(description = "최소 지원 버전. 설치 버전이 이 값 미만이면 강제 업데이트 (major.minor.patch)", example = "1.1.0") String minimumVersion
) {}
