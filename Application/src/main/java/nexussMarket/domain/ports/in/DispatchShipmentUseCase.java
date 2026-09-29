package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;

public interface DispatchShipmentUseCase {

    record Command(String shipmentId) {}

    Shipment execute(User actor, Command command);
}
