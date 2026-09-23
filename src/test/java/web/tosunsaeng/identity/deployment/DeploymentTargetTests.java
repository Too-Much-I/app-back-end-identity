package web.tosunsaeng.identity.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

class DeploymentTargetTests {

	@TempDir Path temp;

	@SuppressWarnings("unchecked")
	private Map<String, Object> workflow() throws Exception {
		return new Yaml().load(Files.readString(Path.of(".github/workflows/deploy-staging.yml")));
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> steps() throws Exception {
		var jobs = (Map<String, Map<String, Object>>) workflow().get("jobs");
		return (List<Map<String, Object>>) jobs.get("deploy").get("steps");
	}

	private Map<String, Object> step(String name) throws Exception {
		return steps().stream().filter(s -> name.equals(s.get("name"))).findFirst().orElseThrow();
	}

	private Result select(String ref, String testRole) throws Exception {
		Path env = Files.createTempFile(temp, "env", ".txt");
		Path output = Files.createTempFile(temp, "output", ".txt");
		var process = new ProcessBuilder("bash", "-c", step("Select deployment target").get("run").toString());
		process.environment().putAll(Map.of("GITHUB_REF", ref, "MAIN_ROLE_ARN", "fake-main-role",
				"TEST_ROLE_ARN", testRole, "GITHUB_ENV", env.toString(), "GITHUB_OUTPUT", output.toString()));
		process.redirectErrorStream(true);
		var running = process.start();
		String log = new String(running.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		return new Result(running.waitFor(), Files.readString(env), Files.readString(output), log);
	}

	@Test void mainKeepsExistingTarget() throws Exception {
		var result = select("refs/heads/main", "");
		assertThat(result.code()).isZero();
		assertThat(result.env()).contains("ECS_SERVICE=tosunsaeng-identity-service\n",
				"TASK_FAMILY=tosunsaeng-identity\n", "IMAGE_CHANNEL=staging\n",
				"HEALTH_URL=https://identity-staging.to-teacher.com/actuator/health\n");
		assertThat(result.output()).isEqualTo("role_arn=fake-main-role\n");
	}

	@Test void developUsesOnlyTestTarget() throws Exception {
		var result = select("refs/heads/develop", "fake-test-role");
		assertThat(result.code()).isZero();
		assertThat(result.env()).contains("ECS_SERVICE=tosunsaeng-identity-test-service\n",
				"TASK_FAMILY=tosunsaeng-identity-test\n", "IMAGE_CHANNEL=test\n",
				"HEALTH_URL=https://identity-test.to-teacher.com/actuator/health\n")
				.doesNotContain("ECS_SERVICE=tosunsaeng-identity-service\n", "IMAGE_CHANNEL=staging");
		assertThat(result.output()).isEqualTo("role_arn=fake-test-role\n");
	}

	@Test void missingTestRoleNeverFallsBackToMain() throws Exception {
		var result = select("refs/heads/develop", "");
		assertThat(result.code()).isNotZero();
		assertThat(result.env()).isEmpty();
		assertThat(result.output()).isEmpty();
	}

	@Test void manualRunsOnOtherBranchesAndTagsAreRejected() throws Exception {
		for (String ref : List.of("refs/heads/feature/test", "refs/tags/main", "refs/heads/devlop")) {
			var result = select(ref, "fake-test-role");
			assertThat(result.code()).isNotZero();
			assertThat(result.env()).isEmpty();
			assertThat(result.output()).isEmpty();
		}
	}

	@Test @SuppressWarnings("unchecked")
	void workflowUsesSelectedTargetsAndChecksTaskBeforeImagePush() throws Exception {
		var workflow = workflow();
		// SnakeYAML YAML 1.1 interprets the unquoted GitHub Actions key 'on' as boolean true.
		var trigger = (Map<String, Object>) workflow.getOrDefault("on", workflow.get(Boolean.TRUE));
		assertThat(((Map<String, Object>) trigger.get("push")).get("branches"))
				.isEqualTo(List.of("main", "develop"));
		assertThat(((Map<String, Object>) workflow.get("concurrency")).get("group"))
				.isEqualTo("identity-deploy-${{ github.ref }}");
		var credentials = (Map<String, Object>) step("Configure AWS credentials").get("with");
		assertThat(credentials.get("role-to-assume")).isEqualTo("${{ steps.target.outputs.role_arn }}");
		var build = (Map<String, Object>) step("Build and push image").get("with");
		assertThat(build.get("tags").toString()).contains("${{ env.IMAGE_CHANNEL }}").doesNotContain(":staging");
		var download = step("Download current task definition");
		assertThat(download.get("run").toString()).contains("set -euo pipefail", "--services \"$ECS_SERVICE\"",
				".family == $family", "select(.name == $container)", "exit 1");
		assertThat(steps().indexOf(download)).isLessThan(steps().indexOf(step("Build and push image")));
		var deploy = (Map<String, Object>) step("Deploy to Amazon ECS").get("with");
		assertThat(deploy.get("service")).isEqualTo("${{ env.ECS_SERVICE }}");
		assertThat(step("Verify health").get("run").toString()).contains("\"$HEALTH_URL\"");
	}

	private record Result(int code, String env, String output, String log) {}
}
