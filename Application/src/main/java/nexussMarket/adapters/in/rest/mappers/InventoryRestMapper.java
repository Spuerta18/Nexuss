package nexussMarket.adapters.in.rest.mappers;

import java.util.List;

import nexussMarket.adapters.in.rest.responses.InventoryItemResponse;
import nexussMarket.domain.models.InventoryItem;

/** Converts inventory items into {@link InventoryItemResponse}. */
public final class InventoryRestMapper {

    private InventoryRestMapper() {
    }

    public static InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(item.getProduct().getIdentifier(), item.getWarehouse().getIdentifier(),
                item.getAvailableQuantity(), item.getReservedQuantity(), item.getDamagedQuantity());
    }

    public static List<InventoryItemResponse> toResponses(List<InventoryItem> items) {
        return items.stream().map(InventoryRestMapper::toResponse).toList();
    }
}
