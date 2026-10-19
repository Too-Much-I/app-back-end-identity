package web.tosunsaeng.identity.domain.auth.mergeprogress;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;

/** Called inside the merge / publisher Mongo transaction, never after commit. */
@Service
@RequiredArgsConstructor
public class MergeProgressStore {
	private final UserMergeProgressRepository repository;
	private final OwnerEventDeliveryRepository deliveries;
	public void capture(OwnerEventCore core, MergeCompletionProfile profile) {
		requireCoverage(core);
		repository.insert(UserMergeProgress.create(core, profile));
	}
	public void confirm(OwnerEventCore core, OwnerEventConsumer consumer, Instant now) {
		requireCoverage(core);
		UserMergeProgress progress = repository.findById(core.getEventId())
				.orElseThrow(() -> new IllegalStateException("Tracked merge progress missing"));
		if (!core.getRequiredConsumers().equals(progress.getRequiredConsumers())
				|| !core.getTargetUserId().equals(progress.getTargetUserId())
				|| !core.getSourceUserId().equals(progress.getSourceUserId()))
			throw new IllegalStateException("Tracked merge progress mismatch");
		progress.confirm(consumer, now);
		// @Version prevents concurrent consumer ACKs from overwriting each other.
		repository.save(progress);
	}
	private void requireCoverage(OwnerEventCore core) {
		var rows = deliveries.findAllByEventId(core.getEventId());
		Set<OwnerEventConsumer> actual = rows.stream().map(OwnerEventDelivery::getConsumer).collect(Collectors.toSet());
		if (rows.size() != actual.size() || !actual.equals(core.getRequiredConsumers()))
			throw new IllegalStateException("Tracked merge delivery coverage mismatch");
	}
}
