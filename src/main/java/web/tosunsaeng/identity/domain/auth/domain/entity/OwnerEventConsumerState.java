package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventCircuitStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;

@Document(collection = "owner_event_consumer_states")
public class OwnerEventConsumerState {
	@Id private OwnerEventConsumer consumer;
	private long lastAllocatedSequence;
	private long lastPublishedSequence;
	private OwnerEventCircuitStatus circuitStatus;
	private OwnerEventFailureCode pauseFailureCode;
	private Instant pausedAt;
	private Instant updatedAt;
	@Version private Long version;

	private OwnerEventConsumerState() {}

	public OwnerEventConsumer getConsumer() { return consumer; }
	public long getLastAllocatedSequence() { return lastAllocatedSequence; }
	public long getLastPublishedSequence() { return lastPublishedSequence; }
	public OwnerEventCircuitStatus getCircuitStatus() { return circuitStatus; }
	public OwnerEventFailureCode getPauseFailureCode() { return pauseFailureCode; }
	public Instant getPausedAt() { return pausedAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }
}
