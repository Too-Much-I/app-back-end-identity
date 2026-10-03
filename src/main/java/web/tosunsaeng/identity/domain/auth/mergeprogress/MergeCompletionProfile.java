package web.tosunsaeng.identity.domain.auth.mergeprogress;

import java.util.Set;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;

public enum MergeCompletionProfile {
	LEARNING_CORE_ONLY(Set.of(OwnerEventConsumer.LEARNING_CORE)),
	LEARNING_CORE_AND_BILLING(Set.of(OwnerEventConsumer.LEARNING_CORE, OwnerEventConsumer.BILLING));

	private final Set<OwnerEventConsumer> consumers;
	MergeCompletionProfile(Set<OwnerEventConsumer> consumers) { this.consumers = consumers; }
	public Set<OwnerEventConsumer> consumers() { return consumers; }
}
