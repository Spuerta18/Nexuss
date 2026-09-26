package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.InventoryItem;

public interface AdjustInventoryUseCase {

    record Command(String productId, String warehouseId, int quantityDelta) {}

    InventoryItem execute(Command command);
}
