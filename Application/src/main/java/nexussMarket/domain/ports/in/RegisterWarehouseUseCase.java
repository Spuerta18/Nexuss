package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

public interface RegisterWarehouseUseCase {

    record Command(String identifier, String name, String address, WarehouseOwnerType ownerType, String sellerId) {}

    Warehouse execute(Command command);
}
