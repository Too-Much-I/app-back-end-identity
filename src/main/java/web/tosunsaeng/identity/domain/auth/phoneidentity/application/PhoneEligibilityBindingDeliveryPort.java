package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

public interface PhoneEligibilityBindingDeliveryPort {

	int deliver(byte[] payload);
}
