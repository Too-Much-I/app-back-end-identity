package web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure;

import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventType;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventDeliveryException;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventDeliveryPort;
import web.tosunsaeng.identity.global.workload.BillingSigV4JsonTransport;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

public final class BillingOwnerEventDeliveryAdapter implements OwnerEventDeliveryPort {
	public static final String USER_MERGED_ROUTE = "/internal/v1/owners/merge/events";
	public static final String TRIAL_REBIND_ROUTE = "/internal/v1/eligibility/trial/owner/events";
	private final BillingSigV4JsonTransport transport;

	public BillingOwnerEventDeliveryAdapter(BillingSigV4JsonTransport transport) {
		this.transport = Objects.requireNonNull(transport);
	}

	@Override
	public WorkloadDeliveryResult deliver(OwnerEventCore event, byte[] payload) {
		String route = event.getEventType() == OwnerEventType.USER_MERGED
				? USER_MERGED_ROUTE : TRIAL_REBIND_ROUTE;
		try {
			return transport.post(route, payload);
		} catch (BillingSigV4JsonTransport.TransportException exception) {
			OwnerEventDeliveryException.Kind kind = switch (exception.kind()) {
				case CREDENTIAL_UNAVAILABLE -> OwnerEventDeliveryException.Kind.CREDENTIAL_UNAVAILABLE;
				case TIMEOUT -> OwnerEventDeliveryException.Kind.TIMEOUT;
				case CONNECTION -> OwnerEventDeliveryException.Kind.CONNECTION;
			};
			throw new OwnerEventDeliveryException(kind, exception);
		}
	}
}
