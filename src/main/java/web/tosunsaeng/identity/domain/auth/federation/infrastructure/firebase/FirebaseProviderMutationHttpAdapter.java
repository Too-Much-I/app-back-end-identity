package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.providerchange.FirebaseProviderMutationPort;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort.Failure;

/** Identity Toolkit deleteProvider targets ONE method. No implicit retry, redirect, delete or disable. */
public final class FirebaseProviderMutationHttpAdapter implements FirebaseProviderMutationPort {
	private final HttpRequestFactory requests;
	private final FirebaseAuthProperties properties;
	private final ObjectMapper json;
	private final String baseUrl;
	public FirebaseProviderMutationHttpAdapter(HttpTransport transport, GoogleCredentials credentials,
			FirebaseAuthProperties properties, ObjectMapper json) {
		this.properties = properties; this.json = json;
		if (!properties.enabled() || !segment(properties.projectId())
				|| (properties.tenantId() != null && !segment(properties.tenantId()))) throw new IllegalArgumentException("Invalid Firebase target configuration.");
		baseUrl = "https://identitytoolkit.googleapis.com/v1/projects/" + properties.projectId()
				+ (properties.tenantId() == null ? "" : "/tenants/" + properties.tenantId());
		var auth = new HttpCredentialsAdapter(credentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")));
		requests = transport.createRequestFactory(request -> {
			auth.initialize(request); request.setNumberOfRetries(0); request.setFollowRedirects(false);
			// The credential interceptor still authenticates the request. Do not refresh credentials
			// in a 401 response handler or replay a mutation after a remote response.
			request.setUnsuccessfulResponseHandler(null);
			request.setConnectTimeout(properties.connectTimeoutMillis()); request.setReadTimeout(properties.readTimeoutMillis());
			request.setWriteTimeout(properties.readTimeoutMillis()); request.setLoggingEnabled(false);
			request.setCurlLoggingEnabled(false); request.setThrowExceptionOnExecuteError(false);
		});
	}
	@Override public Snapshot inspect(FirebaseSessionRevocationPort.Target target) {
		var response = call(target, "/accounts:lookup", Map.of("localId", List.of(target.uid())), false);
		var users = response.path("users");
		if (!users.isArray() || users.size() != 1 || !target.uid().equals(users.get(0).path("localId").asText())) throw failure(Failure.Kind.TARGET_INVALID);
		var user = users.get(0);
		try {
			long created = Long.parseLong(user.path("createdAt").asText());
			long valid = Long.parseLong(user.path("validSince").asText());
			if (created <= 0 || valid < 0) throw failure(Failure.Kind.TARGET_INVALID);
			Map<SocialProvider, String> methods = new EnumMap<>(SocialProvider.class);
			for (var provider : user.path("providerUserInfo")) {
				SocialProvider method = method(provider.path("providerId").asText());
				if (method == null) continue;
				String subject = provider.path("rawId").asText();
				if (subject.isBlank() || methods.put(method, subject) != null) throw failure(Failure.Kind.TARGET_INVALID);
			}
			return new Snapshot(Instant.ofEpochMilli(created), Instant.ofEpochSecond(valid), user.path("disabled").asBoolean(false), methods);
		} catch (RuntimeException e) { throw failure(Failure.Kind.TARGET_INVALID); }
	}
	@Override public void unlink(FirebaseSessionRevocationPort.Target target, SocialProvider provider) {
		String id = providerId(provider);
		if (method(id) != provider) throw failure(Failure.Kind.CONFIGURATION);
		var result = call(target, "/accounts:update", Map.of("localId", target.uid(), "deleteProvider", List.of(id)), true);
		if (!target.uid().equals(result.path("localId").asText())) throw failure(Failure.Kind.RESULT_UNKNOWN);
	}
	@Override public void revoke(FirebaseSessionRevocationPort.Target target, Instant dispatchAt) {
		var result = call(target, "/accounts:update", Map.of("localId", target.uid(), "validSince", dispatchAt.getEpochSecond()), true);
		if (!target.uid().equals(result.path("localId").asText())) throw failure(Failure.Kind.RESULT_UNKNOWN);
	}
	private JsonNode call(FirebaseSessionRevocationPort.Target target, String path, Map<String, ?> body, boolean mutation) {
		if (!properties.projectId().equals(target.projectId()) || !Objects.equals(properties.tenantId(), target.tenantId())
				|| target.uid() == null || target.uid().isBlank()) throw failure(Failure.Kind.CONFIGURATION);
		HttpResponse response = null;
		try {
			var request = requests.buildPostRequest(new GenericUrl(baseUrl + path), new ByteArrayContent("application/json", json.writeValueAsBytes(body)));
			response = request.execute();
			int status = response.getStatusCode();
			if (status < 200 || status >= 300) {
				if (mutation) throw failure(Failure.Kind.RESULT_UNKNOWN);
				throw failure(status == 429 || status >= 500 ? Failure.Kind.READ_TRANSIENT : Failure.Kind.CONFIGURATION);
			}
			byte[] bytes = response.getContent().readNBytes(65537);
			if (bytes.length > 65536) throw failure(mutation ? Failure.Kind.RESULT_UNKNOWN : Failure.Kind.TARGET_INVALID);
			JsonNode result = json.readTree(bytes);
			if (result == null || !result.isObject()) throw failure(mutation ? Failure.Kind.RESULT_UNKNOWN : Failure.Kind.TARGET_INVALID);
			return result;
		} catch (Failure exception) { throw exception; }
		catch (IOException exception) { throw failure(mutation ? Failure.Kind.RESULT_UNKNOWN : Failure.Kind.READ_TRANSIENT); }
		catch (RuntimeException exception) { throw failure(mutation ? Failure.Kind.RESULT_UNKNOWN : Failure.Kind.CONFIGURATION); }
		finally { if (response != null) try { response.disconnect(); } catch (IOException ignored) { } }
	}
	private String providerId(SocialProvider provider) {
		return switch (provider) { case GOOGLE -> "google.com"; case APPLE -> "apple.com"; case KAKAO -> properties.kakaoProviderId(); };
	}
	private SocialProvider method(String value) {
		if ("google.com".equals(value) && properties.googleEnabled()) return SocialProvider.GOOGLE;
		if ("apple.com".equals(value) && properties.appleEnabled()) return SocialProvider.APPLE;
		if (properties.kakaoProviderId().equals(value) && properties.kakaoEnabled()) return SocialProvider.KAKAO;
		return null;
	}
	private static boolean segment(String value) { return value != null && value.matches("[A-Za-z0-9_-]+"); }
	private static Failure failure(Failure.Kind kind) { return new Failure(kind); }
}
