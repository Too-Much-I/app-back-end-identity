package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.util.Objects;


import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingDeliveryException;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingDeliveryPort;
import web.tosunsaeng.identity.global.workload.BillingJsonTransport;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

public final class JdkPhoneEligibilityBindingDeliveryAdapter
		implements PhoneEligibilityBindingDeliveryPort {

	public static final String ROUTE = "/internal/v1/eligibility/trial/events";

	private final web.tosunsaeng.identity.global.workload.BillingJsonTransport transport;

	public JdkPhoneEligibilityBindingDeliveryAdapter(web.tosunsaeng.identity.global.workload.BillingJsonTransport transport) {
		this.transport = Objects.requireNonNull(transport);
	}

	@Override
	public WorkloadDeliveryResult deliver(byte[] payload) {
		try {
			return transport.post(ROUTE, Objects.requireNonNull(payload));
		} catch (BillingJsonTransport.TransportException exception) {
			PhoneEligibilityBindingDeliveryException.Kind kind = switch (exception.kind()) {
				case CREDENTIAL_UNAVAILABLE -> PhoneEligibilityBindingDeliveryException.Kind.CREDENTIAL_UNAVAILABLE;
				case TIMEOUT -> PhoneEligibilityBindingDeliveryException.Kind.TIMEOUT;
				case CONNECTION -> PhoneEligibilityBindingDeliveryException.Kind.CONNECTION;
			};
			throw new PhoneEligibilityBindingDeliveryException(
					kind, exception);
		}
	}
}
