package web.tosunsaeng.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;
import web.tosunsaeng.identity.global.security.jwt.TestRsaKeyConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRsaKeyConfiguration.class)
class OpenApiSharingTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping mappings;
	@MockitoBean private ProviderChangeGuard providerChangeGuard;
	@MockitoBean private UserRepository userRepository;
	@MockitoBean private RefreshSessionRepository refreshSessionRepository;
	@MockitoBean private UserWithdrawnOutboxRepository userWithdrawnOutboxRepository;

	@Test
	void documentsGuestResumeSchemaAndExportsOnlyWhenRequested() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		ObjectNode spec = (ObjectNode) objectMapper.readTree(body);
		JsonNode prepare = spec.path("paths").path("/api/v1/auth/firebase/guest/prepare").path("post");
		assertThat(prepare.path("security").get(0).has("bearerAuth")).isTrue();
		JsonNode envelope = resolve(spec, prepare.path("responses").path("200")
				.path("content").path("application/json").path("schema"));
		JsonNode result = resolve(spec, envelope.path("properties").path("result"));
		assertThat(fieldNames(result.path("properties"))).contains(
				"type", "enrollmentId", "missingRequirements", "privacyConsentVersion",
				"termConsentVersion", "expiresIn");
		assertThat(result.path("properties").path("type").path("enum").toString())
				.contains("ENROLLMENT_REQUIRED", "MERGE_REQUIRED").doesNotContain("ALREADY_LINKED");
		assertThat(prepare.path("responses").has("409")).isTrue();
		assertThat(fieldNames(spec.path("paths"))).contains(
				"/api/v1/auth/logout", "/api/v1/users/withdraw",
				"/api/v1/auth/firebase/providers/link/prepare");
		assertThat(fieldNames(spec.path("paths")))
				.allMatch(path -> path.startsWith("/api/v1/") || path.equals("/.well-known/jwks.json"));
		assertInternalReferencesResolve(spec, spec);
		assertCompleteControllerCoverage(spec);
		assertResponseExamples(spec);

		String outputDirectory = System.getProperty("identity.openapi.output-dir");
		if (outputDirectory == null) {
			return;
		}
		// MockMvc의 localhost는 배포 주소가 아니다. 공유본에는 명시적인 예시 주소만 둔다.
		spec.putArray("servers").addObject()
				.put("url", "https://identity.example.invalid")
				.put("description", "배포 주소 미지정 — 담당자가 전달한 실제 base URL을 사용하세요.");
		Path output = Path.of(outputDirectory);
		Files.createDirectories(output);
		String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
		Files.writeString(output.resolve("identity-openapi.json"), json + "\n", StandardCharsets.UTF_8);
		Files.writeString(output.resolve("openapi.js"), "window.IDENTITY_OPENAPI = "
				+ json.replace("<", "\\u003c") + ";\n", StandardCharsets.UTF_8);
		Files.writeString(output.resolve("GENERATED_AT.txt"),
				Instant.now() + "\nSource: local working tree; not a deployment confirmation.\n",
				StandardCharsets.UTF_8);
	}

	private void assertCompleteControllerCoverage(JsonNode spec) {
		Set<String> actual = new TreeSet<>();
		mappings.getHandlerMethods().forEach((mapping, handler) -> {
			if (!handler.getBeanType().getPackageName().startsWith("web.tosunsaeng.identity.")) return;
			mapping.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods()
					.forEach(method -> actual.add(method.name().toLowerCase(java.util.Locale.ROOT) + " " + path)));
		});
		Set<String> documented = new TreeSet<>();
		spec.path("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(operation -> {
			if (Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace").contains(operation.getKey())) {
				documented.add(operation.getKey() + " " + path.getKey());
			}
		}));
		assertThat(documented).containsExactlyInAnyOrderElementsOf(actual).hasSize(24);
		assertThat(spec.path("paths").size()).isEqualTo(23);
		assertThat(spec.path("paths").has("/api/v1/auth/firebase/providers/relink/prepare")).isFalse();
	}

	private void assertResponseExamples(JsonNode spec) {
		spec.path("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(operation -> {
			JsonNode responses = operation.getValue().path("responses");
			String code = path.getKey().endsWith("/providers/unlink") ? "202" : "200";
			JsonNode media = responses.path(code).path("content").path("application/json");
			assertThat(media.path("examples").size()).as(operation.getKey() + " " + path.getKey()).isPositive();
			assertThat(media.has("schema")).as(path.getKey() + " response schema").isTrue();
			media.path("examples").forEach(example -> {
				JsonNode value = example.path("value");
				if (path.getKey().equals("/.well-known/jwks.json")) {
					assertThat(value.path("keys").isArray()).isTrue();
				} else {
					assertThat(value.path("isSuccess").asBoolean()).isTrue();
					assertThat(value.path("code").asText()).isEqualTo("SUCCESS");
					assertExampleProperties(spec, media.path("schema"), value);
				}
			});
		}));
		JsonNode unlink = spec.path("paths").path("/api/v1/auth/firebase/providers/unlink").path("post").path("responses");
		assertThat(unlink.has("200")).isFalse();
		JsonNode unlinkEnvelope = resolve(spec, unlink.path("202").path("content").path("application/json").path("schema"));
		JsonNode unlinkResult = resolve(spec, unlinkEnvelope.path("properties").path("result"));
		assertThat(fieldNames(unlinkResult.path("properties"))).contains("operationId", "acceptedAt", "completedAt", "nextPollAfterSeconds")
				.doesNotContain("linkAttemptId", "linkAllowed");
		JsonNode link = spec.path("paths").path("/api/v1/auth/firebase/providers/link/start").path("post")
				.path("responses").path("200").path("content").path("application/json");
		JsonNode linkEnvelope = resolve(spec, link.path("schema"));
		assertThat(fieldNames(resolve(spec, linkEnvelope.path("properties").path("result")).path("properties")))
				.contains("linkAttemptId", "expiresAt", "linkAllowed").doesNotContain("operationId", "nextPollAfterSeconds");
		assertThat(link.at("/examples/STARTED/value/result/linkAllowed").asBoolean()).isTrue();
		assertThat(link.at("/examples/START_REPLAY/value/result/linkAllowed").asBoolean()).isFalse();
		JsonNode prepareExamples = spec.path("paths").path("/api/v1/auth/firebase/guest/prepare").path("post")
				.path("responses").path("200").path("content").path("application/json").path("examples");
		assertThat(prepareExamples.at("/MERGE_REQUIRED/value/result").size()).isEqualTo(1);
		assertThat(prepareExamples.at("/RESUME_PROFILE/value/result/missingRequirements").toString()).isEqualTo("[\"PROFILE\"]");
	}

	private void assertExampleProperties(JsonNode spec, JsonNode schema, JsonNode value) {
		JsonNode model = resolve(spec, schema);
		if (model.has("oneOf")) return; // Firebase exchange의 두 구체 타입은 별도 계약 테스트에서 검증한다.
		if (value.isObject() && model.has("properties")) {
			assertThat(fieldNames(value)).isSubsetOf(fieldNames(model.path("properties")));
			value.fields().forEachRemaining(field -> assertExampleProperties(spec, model.path("properties").path(field.getKey()), field.getValue()));
		}
	}

	private JsonNode resolve(JsonNode spec, JsonNode schema) {
		if (!schema.has("$ref")) return schema;
		String reference = schema.path("$ref").asText();
		assertThat(reference).startsWith("#/");
		JsonNode resolved = spec.at(reference.substring(1));
		assertThat(resolved.isMissingNode()).as(reference).isFalse();
		return resolved;
	}

	private void assertInternalReferencesResolve(JsonNode root, JsonNode node) {
		if (node.isObject() && node.has("$ref")) resolve(root, node);
		node.forEach(child -> assertInternalReferencesResolve(root, child));
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> names = new ArrayList<>();
		node.fieldNames().forEachRemaining(names::add);
		return names;
	}
}
