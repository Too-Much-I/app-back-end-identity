package web.tosunsaeng.identity.domain.appversion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;

@Service
public class AppVersionService {
	private final String androidVersion;
	private final String iosVersion;

	public AppVersionService(
			@Value("${app.version.android-latest-version:}") String androidVersion,
			@Value("${app.version.ios-latest-version:}") String iosVersion
	) {
		this.androidVersion = validateVersion(androidVersion);
		this.iosVersion = validateVersion(iosVersion);
	}

	public AppVersionResponse getLatest(String platform) {
		String version;
		if ("android".equals(platform)) {
			version = androidVersion;
		} else if ("ios".equals(platform)) {
			version = iosVersion;
		} else {
			throw new BusinessException(CommonErrorStatus.INVALID_REQUEST);
		}
		if (version.isEmpty()) {
			throw new BusinessException(AppVersionErrorStatus.APP_VERSION_UNAVAILABLE);
		}
		return new AppVersionResponse(platform, version);
	}

	private static String validateVersion(String value) {
		String version = value == null ? "" : value.trim();
		// 미설정은 해당 플랫폼 조회만 503 처리한다. 잘못된 설정은 기동 시 발견한다.
		if (!version.isEmpty()
				&& (version.length() > 32 || !version.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))) {
			throw new IllegalArgumentException("App version must use numeric major.minor.patch format (max 32 characters).");
		}
		return version;
	}
}
