package web.tosunsaeng.identity.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DockerEntrypointTests {
    private static final String FIREBASE = "{\n  \"test_only\": \"not-a-credential\"\n}\n";
    private static final String KEYRING = "test-v1=" + "A".repeat(43) + "=\n";
    @TempDir Path temp;

    @Test
    void jwtOnlyKeepsLegacyStartupAndRestrictsPermissions() throws Exception {
        Result result = run(Map.of());
        assertThat(result.code()).isZero();
        Path boot = bootDirectory();
        assertThat(Files.readString(boot.resolve("private.pem"))).isEqualTo("fake-private\n");
        assertThat(Files.readString(boot.resolve("public.pem"))).isEqualTo("fake-public\n");
        assertPermissions(temp.resolve("secrets"), "rwx------");
        assertPermissions(boot, "rwx------");
        assertPermissions(boot.resolve("private.pem"), "rw-------");
        assertPermissions(boot.resolve("public.pem"), "rw-------");
        assertThat(result.output()).contains("STARTED", "file:" + boot.resolve("private.pem"));
        assertThat(Files.exists(boot.resolve("firebase.json"))).isFalse();
    }

    @Test
    void materializesExactContentsAndDoesNotPassRawSecretsToJvm() throws Exception {
        Result result = run(enabled());
        assertThat(result.code()).isZero();
        Path boot = bootDirectory();
        assertThat(Files.readString(boot.resolve("firebase.json"))).isEqualTo(FIREBASE);
        assertThat(Files.readString(boot.resolve("reissue.properties"))).isEqualTo(KEYRING);
        assertPermissions(boot.resolve("firebase.json"), "rw-------");
        assertPermissions(boot.resolve("reissue.properties"), "rw-------");
        assertThat(result.output()).contains("\n" + boot.resolve("firebase.json") + "\n");
        assertThat(result.output()).contains("\n" + boot.resolve("reissue.properties") + "\n");
    }

    @Test
    void allowsExistingReadOnlyFilesWithoutRewritingThem() throws Exception {
        Path firebase = temp.resolve("mounted.json");
        Path recovery = temp.resolve("mounted.properties");
        Files.writeString(firebase, FIREBASE);
        Files.writeString(recovery, KEYRING);
        Files.setPosixFilePermissions(firebase, PosixFilePermissions.fromString("r--------"));
        Files.setPosixFilePermissions(recovery, PosixFilePermissions.fromString("r--------"));
        Map<String, String> env = enabled();
        env.remove("FIREBASE_SERVICE_ACCOUNT_JSON");
        env.remove("AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT");
        env.put("GOOGLE_APPLICATION_CREDENTIALS", firebase.toString());
        env.put("AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION", recovery.toString());
        Result result = run(env);
        assertThat(result.code()).isZero();
        assertThat(Files.readString(firebase)).isEqualTo(FIREBASE);
        assertThat(Files.readString(recovery)).isEqualTo(KEYRING);
        assertPermissions(firebase, "r--------");
        assertThat(Files.exists(bootDirectory().resolve("firebase.json"))).isFalse();
    }

    static Stream<Arguments> missingRequired() {
        return Stream.of("JWT_PRIVATE_KEY_PEM", "JWT_PUBLIC_KEY_PEM", "FIREBASE_SERVICE_ACCOUNT_JSON",
                "FIREBASE_PROJECT_ID", "AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT",
                "AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID", "AUTH_REISSUE_ENCRYPTION_ENVIRONMENT")
                .map(Arguments::of);
    }

    @ParameterizedTest @MethodSource("missingRequired")
    void rejectsMissingRequiredInputAndCleansGeneratedFiles(String name) throws Exception {
        Map<String, String> env = enabled();
        env.put(name, "");
        Result result = run(env);
        assertThat(result.code()).isNotZero();
        assertThat(result.output()).doesNotContain("STARTED");
        try (var paths = Files.list(temp.resolve("secrets"))) {
            assertThat(paths.toList()).isEmpty();
        }
    }

    static Stream<Arguments> conflictingInputs() {
        return Stream.of("GOOGLE_APPLICATION_CREDENTIALS", "AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION")
                .map(Arguments::of);
    }

    @ParameterizedTest @MethodSource("conflictingInputs")
    void rejectsAmbiguousSources(String pathVariable) throws Exception {
        Map<String, String> env = enabled();
        env.put(pathVariable, temp.resolve("mounted").toString());
        Result result = run(env);
        assertThat(result.code()).isNotZero();
        assertThat(result.output()).contains("choose raw content OR an existing file").doesNotContain("STARTED");
    }

    static Stream<Arguments> invalidPaths() {
        return Stream.of("GOOGLE_APPLICATION_CREDENTIALS", "AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION")
                .flatMap(name -> Stream.of("relative", "file:/tmp/not-used", "/nonexistent/test-only-key")
                        .map(path -> Arguments.of(name, path)));
    }

    @ParameterizedTest @MethodSource("invalidPaths")
    void rejectsInvalidExistingPaths(String name, String path) throws Exception {
        Result result = run(Map.of(name, path));
        assertThat(result.code()).isNotZero();
        assertThat(result.output()).doesNotContain("STARTED");
    }

    @Test
    void rejectsEmptyExistingFile() throws Exception {
        Path empty = Files.createFile(temp.resolve("empty"));
        assertThat(run(Map.of("GOOGLE_APPLICATION_CREDENTIALS", empty.toString())).code()).isNotZero();
    }

    @Test
    void rejectsSymlinkSecretRootWithoutTouchingTarget() throws Exception {
        Path target = Files.createDirectory(temp.resolve("target"));
        Files.createSymbolicLink(temp.resolve("secrets"), target);
        assertThat(run(Map.of()).code()).isNotZero();
        try (var paths = Files.list(target)) { assertThat(paths.toList()).isEmpty(); }
    }

    @Test
    void suppliedCredentialsAreStrippedEvenWhenFeaturesAreOff() throws Exception {
        assertThat(run(Map.of("FIREBASE_SERVICE_ACCOUNT_JSON", FIREBASE,
                "AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT", KEYRING)).code()).isZero();
    }

    private Map<String, String> enabled() {
        Map<String, String> env = new HashMap<>();
        env.put("FIREBASE_AUTH_ENABLED", "true");
        env.put("FIREBASE_PROJECT_ID", "fake-test-project");
        env.put("FIREBASE_SERVICE_ACCOUNT_JSON", FIREBASE);
        env.put("AUTH_REISSUE_RECOVERY_ENABLED", "true");
        env.put("AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT", KEYRING);
        env.put("AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID", "test-v1");
        env.put("AUTH_REISSUE_ENCRYPTION_ENVIRONMENT", "test");
        return env;
    }

    private Result run(Map<String, String> overrides) throws Exception {
        Path bin = Files.createDirectories(temp.resolve("bin"));
        Path java = bin.resolve("java");
        Files.writeString(java, """
                #!/bin/sh
                [ "${JWT_PRIVATE_KEY_PEM+x}" != x ] || exit 81
                [ "${JWT_PUBLIC_KEY_PEM+x}" != x ] || exit 82
                [ "${FIREBASE_SERVICE_ACCOUNT_JSON+x}" != x ] || exit 83
                [ "${AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT+x}" != x ] || exit 84
                [ "$1" = -jar ] && [ "$2" = /app/app.jar ] || exit 85
                printf 'STARTED\\n%s\\n%s\\n%s\\n' "$JWT_PRIVATE_KEY_LOCATION" "${GOOGLE_APPLICATION_CREDENTIALS:-}" "${AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION:-}"
                """);
        Files.setPosixFilePermissions(java, PosixFilePermissions.fromString("rwx------"));
        ProcessBuilder builder = new ProcessBuilder("/bin/sh", Path.of("docker-entrypoint.sh").toAbsolutePath().toString());
        builder.environment().clear(); // Never inherit developer credentials or external config.
        builder.environment().putAll(Map.of("PATH", bin + ":/usr/bin:/bin",
                "IDENTITY_RUNTIME_SECRET_DIR", temp.resolve("secrets").toString(),
                "JWT_PRIVATE_KEY_PEM", "fake-private", "JWT_PUBLIC_KEY_PEM", "fake-public"));
        builder.environment().putAll(overrides);
        Process process = builder.redirectErrorStream(true).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Entrypoint did not terminate");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output).doesNotContain(FIREBASE, KEYRING, "fake-private", "fake-public");
        return new Result(process.exitValue(), output);
    }

    private Path bootDirectory() throws Exception {
        try (var paths = Files.list(temp.resolve("secrets"))) { return paths.findFirst().orElseThrow(); }
    }

    private void assertPermissions(Path path, String permissions) throws Exception {
        assertThat(Files.getPosixFilePermissions(path)).isEqualTo(PosixFilePermissions.fromString(permissions));
    }

    private record Result(int code, String output) { }
}
