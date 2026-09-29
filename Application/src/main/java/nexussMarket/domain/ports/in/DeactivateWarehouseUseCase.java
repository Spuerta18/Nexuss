package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;

public interface DeactivateWarehouseUseCase {

    record Command(String warehouseId) {}

    Warehouse execute(User actor, Command command);
}
