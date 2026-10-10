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

@web.tosunsaeng.identity.domain.auth.mergeprogress.MockMergeProgressInfrastructure
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRsaKeyConfiguration.class)
class OpenApiSharingTests {
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	private web.tosunsaeng.identity.domain.auth.registration.application.GuestRecoveryTransactionService guestRecoveryTransactionService;


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
		JsonNode appVersion = spec.path("paths").path("/api/v1/app/version").path("get");
		assertThat(appVersion.path("security").size()).isZero();
		assertThat(appVersion.path("parameters").get(0).path("required").asBoolean()).isTrue();
		assertThat(appVersion.path("parameters").get(0).path("schema").path("enum").toString())
				.contains("android", "ios");
		assertThat(appVersion.path("responses").has("400")).isTrue();
		assertThat(appVersion.path("responses").has("503")).isTrue();
		JsonNode versionEnvelope = resolve(spec, appVersion.path("responses").path("200")
				.path("content").path("application/json").path("schema"));
		JsonNode versionResult = resolve(spec, versionEnvelope.path("properties").path("result"));
		assertThat(fieldNames(versionResult.path("properties")))
				.containsExactlyInAnyOrder("platform", "latestVersion", "minimumVersion");
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
		JsonNode mergeResponses = spec.path("paths").path("/api/v1/auth/firebase/guest/merge")
				.path("post").path("responses");
		JsonNode mergeForbiddenExamples = mergeResponses.path("403").path("content")
				.path("application/json").path("examples");
		assertThat(mergeForbiddenExamples.path("GUEST_MERGE_TARGET_WITHDRAWN").path("value")
				.path("code").asText()).isEqualTo("GUEST_MERGE_TARGET_WITHDRAWN");
		assertThat(mergeForbiddenExamples.path("GUEST_MERGE_TARGET_NOT_ACTIVE").path("value")
				.path("code").asText()).isEqualTo("GUEST_MERGE_TARGET_NOT_ACTIVE");
		assertThat(fieldNames(spec.path("paths"))).contains(
				"/api/v1/auth/logout", "/api/v1/users/withdraw",
				"/api/v1/auth/firebase/providers/link/prepare");
		assertThat(fieldNames(spec.path("paths")))
				.allMatch(path -> path.startsWith("/api/v1/") || path.equals("/.well-known/jwks.json"));
		assertInternalReferencesResolve(spec, spec);
		assertCompleteControllerCoverage(spec);
		assertResponseExamples(spec);
		assertWithdrawalDocumentation(spec);

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

	@Test
	void reissueOffersNamedUnauthorizedExamplesWithoutChangingSuccess() throws Exception {
		JsonNode spec = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
		JsonNode operation = spec.path("paths").path("/api/v1/auth/reissue").path("post");
		assertThat(operation.path("description").asText()).contains("임시 호환", "GUEST·MEMBER 구분 없이", "401 INVALID_REFRESH_TOKEN");
		JsonNode media = operation.at("/responses/401/content/application~1json");
		assertThat(media.path("schema").path("$ref").asText()).isEqualTo("#/components/schemas/ApiErrorResponse");
		assertThat(media.hasNonNull("example")).isFalse();
		var codes = List.of(
				web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.INVALID_REFRESH_TOKEN,
				web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.REFRESH_TOKEN_EXPIRED,
				web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED,
				web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.ACCOUNT_WITHDRAWN);
		assertThat(fieldNames(media.path("examples"))).containsExactlyInAnyOrderElementsOf(codes.stream().map(c -> c.getCode()).toList());
		for (var code : codes) {
			JsonNode example = media.path("examples").path(code.getCode());
			assertThat(example.path("summary").asText()).contains(code.getCode());
			assertThat(example.path("description").asText()).isNotBlank();
			assertThat(code.getHttpStatus().value()).isEqualTo(401);
			assertThat(example.at("/value/isSuccess").asBoolean()).isFalse();
			assertThat(example.at("/value/code").asText()).isEqualTo(code.getCode());
			assertThat(example.at("/value/message").asText()).isEqualTo(code.getMessage());
			assertThat(example.at("/value/result").isNull()).isTrue();
			assertThat(example.path("value").has("data")).isFalse();
		}
		assertThat(operation.at("/responses/200/content/application~1json/examples/ROTATED/value/isSuccess").asBoolean()).isTrue();
	}

	private void assertWithdrawalDocumentation(JsonNode spec) {
		JsonNode operation = spec.path("paths").path("/api/v1/users/withdraw").path("post");
		assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
		assertThat(operation.path("description").asText()).contains("최근 SNS 재로그인은 요구하지 않습니다");
		JsonNode requestMedia = operation.at("/requestBody/content/application~1json");
		assertThat(requestMedia.has("example")).isFalse();
		assertThat(fieldNames(requestMedia.path("examples"))).containsExactlyInAnyOrder("SNS", "LOCAL", "GUEST");
		assertThat(fieldNames(requestMedia.at("/examples/SNS/value"))).containsExactlyInAnyOrder("refreshToken", "firebaseIdToken");
		assertThat(fieldNames(requestMedia.at("/examples/LOCAL/value"))).containsExactlyInAnyOrder("refreshToken", "password");
		assertThat(fieldNames(requestMedia.at("/examples/GUEST/value"))).containsExactly("refreshToken");
		JsonNode requestSchema = resolve(spec, requestMedia.path("schema"));
		assertThat(requestSchema.path("required").toString()).isEqualTo("[\"refreshToken\"]");
		for (String field : List.of("refreshToken", "password", "firebaseIdToken")) {
			assertThat(requestSchema.path("properties").path(field).path("writeOnly").asBoolean()).isTrue();
		}
		java.util.Map<String, web.tosunsaeng.identity.global.exception.ErrorCode> codes = new java.util.HashMap<>();
		for (var values : List.of(web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.values(),
				web.tosunsaeng.identity.domain.user.exception.UserErrorStatus.values(),
				web.tosunsaeng.identity.global.exception.CommonErrorStatus.values())) {
			for (var code : values) codes.put(code.getCode(), code);
		}
		assertThat(fieldNames(operation.path("responses"))).containsExactlyInAnyOrder("200", "400", "401", "403", "404", "409", "429", "503");
		operation.path("responses").fields().forEachRemaining(response -> {
			if (response.getKey().equals("200")) return;
			JsonNode media = response.getValue().at("/content/application~1json");
			assertThat(media.path("schema").path("$ref").asText()).isEqualTo("#/components/schemas/ApiErrorResponse");
			assertThat(media.path("examples").size()).isPositive();
			media.path("examples").fields().forEachRemaining(example -> {
				JsonNode value = example.getValue().path("value");
				assertThat(value.path("isSuccess").asBoolean()).isFalse();
				var code = codes.get(value.path("code").asText());
				assertThat(code).as(example.getKey()).isNotNull();
				assertThat(code.getHttpStatus().value()).isEqualTo(Integer.parseInt(response.getKey()));
				assertThat(value.path("message").asText()).isEqualTo(code.getMessage());
				if (example.getKey().equals("VALIDATION_ERROR")) {
					assertThat(value.path("result").isArray()).isTrue();
					assertThat(value.at("/result/0/rejectedValue").isNull()).isTrue();
					assertThat(value.at("/result/0/field").asText()).isEqualTo("refreshToken");
				} else assertThat(value.path("result").isNull()).isTrue();
			});
		});
		assertThat(spec.at("/components/schemas/ApiErrorResponse/properties/isSuccess/example").asBoolean()).isFalse();
		assertThat(spec.at("/components/schemas/BaseResponse/properties/isSuccess/example").asBoolean()).isTrue();
		assertThat(operation.at("/responses/200/content/application~1json/examples/WITHDRAWN/value/isSuccess").asBoolean()).isTrue();
		assertThat(operation.at("/responses/400/content/application~1json/examples/WITHDRAWAL_FIREBASE_PROOF_REQUIRED/value/message").asText())
				.contains("유효한").doesNotContain("최근");
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
		assertThat(documented).containsExactlyInAnyOrderElementsOf(actual).hasSize(34);
		assertThat(spec.path("paths").size()).isEqualTo(33);
		assertThat(spec.path("paths").has("/api/v1/auth/firebase/providers/relink/prepare")).isFalse();
	}

	private void assertResponseExamples(JsonNode spec) {
		spec.path("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(operation -> {
			JsonNode responses = operation.getValue().path("responses");
			String code = path.getKey().contains("/providers/") ? "410" : "200";
			JsonNode media = responses.path(code).path("content").path("application/json");
			assertThat(media.path("examples").size()).as(operation.getKey() + " " + path.getKey()).isPositive();
			assertThat(media.has("schema")).as(path.getKey() + " response schema").isTrue();
			media.path("examples").forEach(example -> {
				JsonNode value = example.path("value");
				if (path.getKey().equals("/.well-known/jwks.json")) {
					assertThat(value.path("keys").isArray()).isTrue();
				} else if (code.equals("410")) {
					assertThat(value.path("isSuccess").asBoolean()).isFalse();
					assertThat(value.path("code").asText()).isEqualTo("PROVIDER_LINK_RETIRED");
				} else {
					assertThat(value.path("isSuccess").asBoolean()).isTrue();
					assertThat(value.path("code").asText()).isEqualTo("SUCCESS");
					assertExampleProperties(spec, media.path("schema"), value);
				}
			});
		}));
		JsonNode unlink = spec.path("paths").path("/api/v1/auth/firebase/providers/unlink").path("post").path("responses");
		assertThat(unlink.has("200")).isFalse();
		assertThat(unlink.has("202")).isFalse();
		assertThat(unlink.has("410")).isTrue();
		JsonNode link = spec.path("paths").path("/api/v1/auth/firebase/providers/link/start").path("post")
				.path("responses");
		assertThat(link.has("200")).isFalse();
		assertThat(link.has("410")).isTrue();
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
