package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;

public interface OwnerEventCoreRepositoryCustom {
	boolean markAllPublished(String eventId, Instant allPublishedAt, Instant cleanupAt);
}
