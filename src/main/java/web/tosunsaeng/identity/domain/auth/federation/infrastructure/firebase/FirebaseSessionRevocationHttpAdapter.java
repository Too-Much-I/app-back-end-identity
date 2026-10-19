package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort;

/** Same Identity Toolkit endpoints as Admin SDK 9.4.3, without its implicit retries. */
public final class FirebaseSessionRevocationHttpAdapter implements FirebaseSessionRevocationPort {
	private final HttpRequestFactory requests;
	private final FirebaseAuthProperties properties;
	private final ObjectMapper json;
	private final String baseUrl;

	public FirebaseSessionRevocationHttpAdapter(HttpTransport transport, GoogleCredentials credentials,
			FirebaseAuthProperties properties, ObjectMapper json) {
		this.properties = Objects.requireNonNull(properties); this.json = Objects.requireNonNull(json);
		if (!properties.enabled() || !segment(properties.projectId())
				|| (properties.tenantId() != null && !segment(properties.tenantId()))) {
			throw new IllegalArgumentException("Invalid Firebase revocation target configuration.");
		}
		this.baseUrl = "https://identitytoolkit.googleapis.com/v1/projects/" + properties.projectId()
				+ (properties.tenantId() == null ? "" : "/tenants/" + properties.tenantId());
		var auth = new HttpCredentialsAdapter(credentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")));
		this.requests = transport.createRequestFactory(request -> {
			auth.initialize(request);
			request.setNumberOfRetries(0);
			request.setFollowRedirects(false);
			request.setConnectTimeout(properties.connectTimeoutMillis());
			request.setReadTimeout(properties.readTimeoutMillis());
			request.setWriteTimeout(properties.readTimeoutMillis());
			request.setLoggingEnabled(false);
			request.setCurlLoggingEnabled(false);
			request.setThrowExceptionOnExecuteError(false);
		});
	}

	@Override public Snapshot inspect(Target target) {
		JsonNode response = call(target, "/accounts:lookup", Map.of("localId", List.of(target.uid())), false);
		JsonNode users = response.path("users");
		if (!users.isArray() || users.size() != 1 || !target.uid().equals(users.get(0).path("localId").asText())) throw failure(Failure.Kind.TARGET_INVALID);
		JsonNode user = users.get(0);
		try {
			long created = Long.parseLong(user.path("createdAt").asText());
			long valid = Long.parseLong(user.path("validSince").asText());
			if (created <= 0 || valid < 0) throw failure(Failure.Kind.TARGET_INVALID);
			return new Snapshot(Instant.ofEpochMilli(created), Instant.ofEpochSecond(valid), user.path("disabled").asBoolean(false));
		} catch (RuntimeException exception) { throw failure(Failure.Kind.TARGET_INVALID); }
	}

	@Override public void revoke(Target target, Instant dispatchAt) {
		JsonNode response = call(target, "/accounts:update", Map.of("localId", target.uid(),
				"validSince", dispatchAt.getEpochSecond()), true);
		if (!target.uid().equals(response.path("localId").asText())) throw failure(Failure.Kind.RESULT_UNKNOWN);
	}

	private JsonNode call(Target target, String path, Map<String, ?> payload, boolean mutation) {
		if (!properties.projectId().equals(target.projectId()) || !Objects.equals(properties.tenantId(), target.tenantId())
				|| target.uid() == null || target.uid().isBlank()) throw failure(Failure.Kind.CONFIGURATION);
		HttpResponse response = null;
		try {
			var request = requests.buildPostRequest(new GenericUrl(baseUrl + path),
					new ByteArrayContent("application/json", json.writeValueAsBytes(payload)));
			response = request.execute();
			int status = response.getStatusCode();
			if (status < 200 || status >= 300) {
				if (mutation) throw failure(Failure.Kind.RESULT_UNKNOWN);
				throw failure(status == 429 || status >= 500 ? Failure.Kind.READ_TRANSIENT : Failure.Kind.CONFIGURATION);
			}
			byte[] bytes = response.getContent().readNBytes(65537);
			if (bytes.length > 65536) throw failure(mutation ? Failure.Kind.RESULT_UNKNOWN : Failure.Kind.TARGET_INVALID);
			return json.readTree(bytes);
		} catch (IOException exception) {
			throw failure(mutation ? Failure.Kind.RESULT_UNKNOWN : Failure.Kind.READ_TRANSIENT);
		} finally {
			if (response != null) try { response.disconnect(); } catch (IOException ignored) { }
		}
	}
	private static boolean segment(String value) { return value != null && value.matches("[A-Za-z0-9_-]+"); }
	private static Failure failure(Failure.Kind kind) { return new Failure(kind); }
}
