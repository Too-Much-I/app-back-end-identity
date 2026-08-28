package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

public interface UserWithdrawnDeliveryPort {

	int deliver(byte[] payload);
}
