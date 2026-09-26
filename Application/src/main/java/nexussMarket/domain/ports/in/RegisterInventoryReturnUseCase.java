package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.InventoryItem;

public interface RegisterInventoryReturnUseCase {

    record Command(String productId, String warehouseId, int quantity) {}

    InventoryItem execute(Command command);
}
