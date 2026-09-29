package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.User;

public interface RegisterInventoryInboundUseCase {

    record Command(String productId, String warehouseId, int quantity) {}

    InventoryItem execute(User actor, Command command);
}
