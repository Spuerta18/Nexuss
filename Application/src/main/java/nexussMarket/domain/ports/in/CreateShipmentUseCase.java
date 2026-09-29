package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;

public interface CreateShipmentUseCase {

    record Command(String shipmentId, String orderId) {}

    Shipment execute(User actor, Command command);
}
