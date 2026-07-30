package web.tosunsaeng.identity.global.security.jwt;

import java.util.List;
import java.util.Objects;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_TOKEN,
			"The required audience is missing.",
			null
	);

	private final String requiredAudience;

	public JwtAudienceValidator(String requiredAudience) {
		this.requiredAudience = Objects.requireNonNull(
				requiredAudience,
				"requiredAudience must not be null"
		);
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		List<String> audiences = jwt == null ? null : jwt.getAudience();
		if (audiences != null && audiences.contains(requiredAudience)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
	}
}
