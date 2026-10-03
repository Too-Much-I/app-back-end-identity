package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

public interface RecoveryStore {
	void prepare(String id, Instant now, Instant expiresAt);
	void admit(List<String> keys, int windowSeconds, int limit, Instant now);
	RecoveryResult lookup(String id, List<String> proofIds, Instant authTime, Instant now,
			Duration retryTtl, Supplier<RecoveryResult> resolve);
}
