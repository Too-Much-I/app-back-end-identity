package web.tosunsaeng.identity.domain.appversion;

import java.math.BigInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;

@Service
public class AppVersionService {
	private final String androidVersion;
	private final String iosVersion;
	private final String androidMinimumVersion;
	private final String iosMinimumVersion;

	public AppVersionService(
			@Value("${app.version.android-latest-version:}") String androidVersion,
			@Value("${app.version.ios-latest-version:}") String iosVersion,
			@Value("${app.version.android-minimum-version:}") String androidMinimumVersion,
			@Value("${app.version.ios-minimum-version:}") String iosMinimumVersion
	) {
		this.androidVersion = validateVersion(androidVersion);
		this.iosVersion = validateVersion(iosVersion);
		this.androidMinimumVersion = validateVersion(androidMinimumVersion);
		this.iosMinimumVersion = validateVersion(iosMinimumVersion);
		validateRange(this.androidMinimumVersion, this.androidVersion);
		validateRange(this.iosMinimumVersion, this.iosVersion);
	}

	public AppVersionResponse getLatest(String platform) {
		String version;
		String minimumVersion;
		if ("android".equals(platform)) {
			version = androidVersion;
			minimumVersion = androidMinimumVersion;
		} else if ("ios".equals(platform)) {
			version = iosVersion;
			minimumVersion = iosMinimumVersion;
		} else {
			throw new BusinessException(CommonErrorStatus.INVALID_REQUEST);
		}
		if (version.isEmpty() || minimumVersion.isEmpty()) {
			throw new BusinessException(AppVersionErrorStatus.APP_VERSION_UNAVAILABLE);
		}
		return new AppVersionResponse(platform, version, minimumVersion);
	}

	private static void validateRange(String minimum, String latest) {
		if (minimum.isEmpty() || latest.isEmpty()) return;
		String[] lower = minimum.split("\\.");
		String[] upper = latest.split("\\.");
		for (int i = 0; i < 3; i++) {
			int comparison = new BigInteger(lower[i]).compareTo(new BigInteger(upper[i]));
			if (comparison < 0) return;
			if (comparison > 0) {
				throw new IllegalArgumentException("Minimum app version must not exceed latest app version.");
			}
		}
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
