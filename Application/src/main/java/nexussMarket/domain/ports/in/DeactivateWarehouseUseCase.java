package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Warehouse;

public interface DeactivateWarehouseUseCase {

    record Command(String warehouseId) {}

    Warehouse execute(Command command);
}
