package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

public interface PhoneEligibilityBindingDeliveryPort {

	WorkloadDeliveryResult deliver(byte[] payload);
}
