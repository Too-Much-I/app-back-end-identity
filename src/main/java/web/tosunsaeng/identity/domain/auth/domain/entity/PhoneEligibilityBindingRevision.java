package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "phone_eligibility_binding_revisions")
@CompoundIndex(
		name = "uk_phone_eligibility_revision_user_scope",
		def = "{ 'userId': 1, 'consumerScopeId': 1 }",
		unique = true
)
public class PhoneEligibilityBindingRevision {

	@Id
	private String bindingId;

	private String userId;

	private String consumerScopeId;

	private long revision;

	private boolean active;

	private Instant updatedAt;

	private PhoneEligibilityBindingRevision() {
	}

	public String getBindingId() {
		return bindingId;
	}

	public String getUserId() {
		return userId;
	}

	public String getConsumerScopeId() {
		return consumerScopeId;
	}

	public long getRevision() {
		return revision;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
