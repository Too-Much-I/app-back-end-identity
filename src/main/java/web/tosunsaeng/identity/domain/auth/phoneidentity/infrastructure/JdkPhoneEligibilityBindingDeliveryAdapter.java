package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingDeliveryException;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingDeliveryPort;
import web.tosunsaeng.identity.global.workload.BillingSigV4JsonTransport;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

public final class JdkPhoneEligibilityBindingDeliveryAdapter
		implements PhoneEligibilityBindingDeliveryPort {

	public static final String ROUTE = "/internal/v1/eligibility/trial/events";

	private final BillingSigV4JsonTransport transport;

	public JdkPhoneEligibilityBindingDeliveryAdapter(
			URI baseUrl, String region, Duration connectTimeout, Duration readTimeout,
			AwsCredentialsProvider credentialProvider
	) {
		this.transport = new BillingSigV4JsonTransport(
				baseUrl, region, connectTimeout, readTimeout, credentialProvider);
	}

	@Override
	public WorkloadDeliveryResult deliver(byte[] payload) {
		try {
			return transport.post(ROUTE, Objects.requireNonNull(payload));
		} catch (BillingSigV4JsonTransport.TransportException exception) {
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
