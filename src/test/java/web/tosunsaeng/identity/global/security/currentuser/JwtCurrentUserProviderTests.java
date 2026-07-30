package web.tosunsaeng.identity.global.security.currentuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;

class JwtCurrentUserProviderTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant NOW = Instant.parse("2026-07-27T01:02:03Z");

	private final JwtCurrentUserProvider currentUserProvider = new JwtCurrentUserProvider();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void returnsCanonicalUuidSubjectFromJwtAuthenticationToken() {
		JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt(USER_ID), List.of());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertThat(currentUserProvider.getCurrentUserId()).isEqualTo(USER_ID);
	}

	@Test
	void acceptsAuthenticatedPrincipalThatIsJwt() {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				jwt(USER_ID),
				null,
				List.of()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertThat(currentUserProvider.getCurrentUserId()).isEqualTo(USER_ID);
	}

	@Test
	void rejectsMissingAuthenticationAndNonJwtPrincipal() {
		assertUnauthorized(currentUserProvider::getCurrentUserId);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				"not-a-jwt-principal",
				null,
				List.of()
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertUnauthorized(currentUserProvider::getCurrentUserId);
	}

	@Test
	void rejectsUnauthenticatedJwtAuthentication() {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				jwt(USER_ID),
				null
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertUnauthorized(currentUserProvider::getCurrentUserId);
	}

	@Test
	void rejectsMissingOrNonCanonicalUuidSubjectWithSafeUnauthorizedError() {
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(null)));
		assertUnauthorized(currentUserProvider::getCurrentUserId);

		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt("not-a-uuid"))
		);
		assertUnauthorized(currentUserProvider::getCurrentUserId);
	}

	private Jwt jwt(String subject) {
		Jwt.Builder builder = Jwt.withTokenValue("current-user-provider-test-value")
				.header("alg", "RS256")
				.issuedAt(NOW)
				.expiresAt(NOW.plusSeconds(60));
		if (subject != null) {
			builder.subject(subject);
		}
		return builder.build();
	}

	private void assertUnauthorized(Runnable invocation) {
		BusinessException exception = catchThrowableOfType(BusinessException.class, invocation::run);

		assertThat(exception.getErrorCode()).isEqualTo(CommonErrorStatus.UNAUTHORIZED);
		assertThat(exception.getMessage()).isEqualTo("인증이 필요합니다.");
	}
}
