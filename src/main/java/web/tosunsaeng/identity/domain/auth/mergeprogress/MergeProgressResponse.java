package web.tosunsaeng.identity.domain.auth.mergeprogress;
import java.time.Instant;
import java.util.List;
public record MergeProgressResponse(String mergeId, Status status, Instant createdAt, Instant completedAt,
		Component learningCore, Component billing, Integer nextPollAfterSeconds) {
	public enum Status { PROCESSING, ACTION_REQUIRED, COMPLETED }
	public enum ComponentStatus { PENDING, COMPLETED, ACTION_REQUIRED, NOT_REQUIRED }
	public record Component(ComponentStatus status, Instant confirmedAt) {}
	public record Page(List<MergeProgressResponse> items, String nextCursor) {}
}
