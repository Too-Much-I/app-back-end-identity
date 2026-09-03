package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

public interface OwnerEventDeliveryPort {
	WorkloadDeliveryResult deliver(OwnerEventCore event, byte[] payload);
}
