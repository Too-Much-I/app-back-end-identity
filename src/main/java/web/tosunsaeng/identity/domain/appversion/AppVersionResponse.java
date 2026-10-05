package web.tosunsaeng.identity.domain.appversion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "플랫폼별 최신 출시 앱 버전. 업데이트 판단은 앱에서 수행합니다.")
public record AppVersionResponse(
		@Schema(allowableValues = {"android", "ios"}, example = "android") String platform,
		@Schema(description = "최신 출시 버전 (major.minor.patch)", example = "1.2.0") String latestVersion
) {}
