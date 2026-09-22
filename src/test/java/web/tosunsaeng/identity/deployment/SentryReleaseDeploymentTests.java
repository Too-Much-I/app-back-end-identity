package web.tosunsaeng.identity.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import web.tosunsaeng.identity.global.observability.SentryEventSanitizer;
import io.sentry.SentryEvent;
import io.sentry.Hint;

class SentryReleaseDeploymentTests {
	@Test @SuppressWarnings("unchecked")
	void existingRenderActionPassesReleaseMatchingImmutableImageCommit() throws Exception {
		Map<String, Object> workflow = new Yaml().load(Files.readString(Path.of(".github/workflows/deploy-staging.yml")));
		Map<String, Object> env = (Map<String, Object>) workflow.get("env");
		assertThat(env.get("SENTRY_RELEASE")).isEqualTo("app-back-end-identity@${{ github.sha }}");
		var jobs = (Map<String, Map<String, Object>>) workflow.get("jobs");
		var steps = (List<Map<String, Object>>) jobs.get("deploy").get("steps");
		var render = steps.stream().filter(s -> "render-task-definition".equals(s.get("id"))).findFirst().orElseThrow();
		assertThat(render.get("uses")).isEqualTo("aws-actions/amazon-ecs-render-task-definition@v1");
		var inputs = (Map<String, Object>) render.get("with");
		assertThat(inputs.get("image")).isEqualTo("${{ steps.image.outputs.image_uri }}");
		assertThat(inputs.get("task-definition")).isEqualTo("task-definition.json");
		assertThat(inputs.get("container-name")).isEqualTo("${{ env.CONTAINER_NAME }}");
		assertThat(inputs.get("environment-variables").toString().trim()).isEqualTo("SENTRY_RELEASE=${{ env.SENTRY_RELEASE }}");
		assertThat(inputs).doesNotContainKeys("secrets", "log-configuration-options", "docker-labels");
		var image = steps.stream().filter(s -> "image".equals(s.get("id"))).findFirst().orElseThrow();
		assertThat(image.get("run").toString()).contains("${ECR_REGISTRY}/${ECR_REPOSITORY}:${GITHUB_SHA}");
	}
	@Test void sanitizerPreservesConfiguredReleaseNotEventSuppliedVersion() {
		String expected = "app-back-end-identity@0123456789012345678901234567890123456789";
		SentryEvent event = new SentryEvent(); event.setRelease("untrusted");
		var result = new SentryEventSanitizer("test", expected).execute(event, new Hint());
		assertThat(result.getRelease()).isEqualTo(expected);
	}
}
