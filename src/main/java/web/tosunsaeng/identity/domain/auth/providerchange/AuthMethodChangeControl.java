package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

/** Security state, deliberately NOT subject to operation-history TTL. No credential/subject copies. */
@Getter
@Document("auth_method_change_controls")
public class AuthMethodChangeControl {
	@Id private String userId;
	@Version private Long version;
	private String bindingId;
	private long revision;
	private Map<String, Instant> blocked = new HashMap<>();
	private Map<String, Instant> authenticationFloors = new HashMap<>();
	private AuthMethodChangeControl() { }
	public AuthMethodChangeControl(String userId, String bindingId) {
		this.userId = userId;
		this.bindingId = bindingId;
	}
	public boolean isBlocked(SocialProvider provider) { return blocked.containsKey(provider.name()); }
	public void block(SocialProvider provider, Instant now) {
		blocked.put(provider.name(), now);
		revision = Math.incrementExact(revision);
	}
	public void release(SocialProvider provider, Instant proofFloor) {
		blocked.remove(provider.name());
		authenticationFloors.put(provider.name(), proofFloor);
		revision = Math.incrementExact(revision);
	}
}
