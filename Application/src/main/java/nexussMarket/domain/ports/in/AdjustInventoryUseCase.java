package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.User;

public interface AdjustInventoryUseCase {

    record Command(String productId, String warehouseId, int quantityDelta) {}

    InventoryItem execute(User actor, Command command);
}
