package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RecoveryHasherTests {
	String key = Base64.getEncoder().encodeToString(new byte[32]);
	@Test void separatesPurposesAndUnambiguousComponents() {
		var hasher = new RecoveryHasher("old:" + key);
		assertThat(hasher.hashes("proof", "ab", "c")).isNotEqualTo(hasher.hashes("proof", "a", "bc"))
				.isNotEqualTo(hasher.hashes("budget", "ab", "c"));
		assertThat(hasher.toString()).doesNotContain(key);
	}
	@Test void retainedKeysAllowRotationWithoutReplayReset() {
		var old = new RecoveryHasher("old:" + key);
		var next = new RecoveryHasher("new:" + Base64.getEncoder().encodeToString("test-only-new-key-32-bytes-long!!!".getBytes()) + ",old:" + key);
		assertThat(next.hashes("proof", "uid")).containsAll(old.hashes("proof", "uid")).hasSize(2);
	}
	@Test void invalidKeysFailClosedWithoutLeakingInput() {
		for (String invalid : new String[]{"", "v:private-key", "v:AA==", "v:" + key + ",v:" + key}) {
			assertThatThrownBy(() -> new RecoveryHasher(invalid)).isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Invalid account recovery key ring");
		}
	}
}
