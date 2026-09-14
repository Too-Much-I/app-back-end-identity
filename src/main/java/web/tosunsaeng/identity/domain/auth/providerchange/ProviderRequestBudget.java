package web.tosunsaeng.identity.domain.auth.providerchange;

import java.util.HashMap;
import java.util.Map;

/** Per-instance defense in depth. Synchronized admission bounds cardinality even under concurrent new keys. */
final class ProviderRequestBudget {
	private record Bucket(long minute, int count) { }
	private final Map<String, Bucket> buckets = new HashMap<>();
	private final int capacity;
	private final int limit;
	ProviderRequestBudget(int capacity, int limit) { this.capacity = capacity; this.limit = limit; }
	synchronized boolean admit(String key, long minute) {
		if (buckets.size() >= capacity) buckets.entrySet().removeIf(e -> e.getValue().minute() < minute);
		var old = buckets.get(key);
		if (old == null && buckets.size() >= capacity) return false;
		int count = old == null || old.minute() != minute ? 1 : Math.min(limit + 1, old.count() + 1);
		buckets.put(key, new Bucket(minute, count));
		return count <= limit;
	}
}
