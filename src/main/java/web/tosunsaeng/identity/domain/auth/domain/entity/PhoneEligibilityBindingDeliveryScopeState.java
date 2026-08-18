package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingFailureCode;

@Document(collection = "phone_eligibility_binding_delivery_scopes")
public class PhoneEligibilityBindingDeliveryScopeState {

	@Id
	private String consumerScopeId;

	private PhoneEligibilityBindingFailureCode failureCode;

	private Instant pausedAt;

	private PhoneEligibilityBindingDeliveryScopeState() {
	}

	private PhoneEligibilityBindingDeliveryScopeState(
			String consumerScopeId,
			PhoneEligibilityBindingFailureCode failureCode,
			Instant pausedAt
	) {
		this.consumerScopeId = requireScope(consumerScopeId);
		this.failureCode = Objects.requireNonNull(failureCode, "failureCode must not be null");
		this.pausedAt = Objects.requireNonNull(pausedAt, "pausedAt must not be null");
	}

	public static PhoneEligibilityBindingDeliveryScopeState paused(
			String consumerScopeId,
			PhoneEligibilityBindingFailureCode failureCode,
			Instant pausedAt
	) {
		return new PhoneEligibilityBindingDeliveryScopeState(
				consumerScopeId,
				failureCode,
				pausedAt
		);
	}

	private static String requireScope(String value) {
		String required = Objects.requireNonNull(value, "consumerScopeId must not be null");
		if (!required.matches("[A-Za-z0-9._:-]{1,128}")) {
			throw new IllegalArgumentException("consumerScopeId has an invalid format");
		}
		return required;
	}

	public String getConsumerScopeId() {
		return consumerScopeId;
	}

	public PhoneEligibilityBindingFailureCode getFailureCode() {
		return failureCode;
	}

	public Instant getPausedAt() {
		return pausedAt;
	}
}
