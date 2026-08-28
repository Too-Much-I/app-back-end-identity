package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;

public final class UserWithdrawnReplayService {

	private final UserWithdrawnOutboxRepository repository;
	private final Clock clock;

	public UserWithdrawnReplayService(
			UserWithdrawnOutboxRepository repository,
			Clock clock
	) {
		this.repository = Objects.requireNonNull(repository);
		this.clock = Objects.requireNonNull(clock);
	}

	public boolean replay(String eventId) {
		String canonicalEventId = UUID.fromString(eventId).toString();
		if (!canonicalEventId.equals(eventId)) {
			throw new IllegalArgumentException("eventId must be a lowercase canonical UUID.");
		}
		return repository.replayDeadLetter(canonicalEventId, clock.instant());
	}
}
