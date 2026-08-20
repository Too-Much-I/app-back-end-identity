package web.tosunsaeng.identity.domain.auth.usermerge.application;

public interface UserMergedDeliveryPort {

	int deliver(byte[] payload);
}
