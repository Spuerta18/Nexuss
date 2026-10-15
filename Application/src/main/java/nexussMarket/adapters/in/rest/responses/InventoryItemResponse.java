package nexussMarket.adapters.in.rest.responses;

/** The stock of a product in one warehouse. */
public record InventoryItemResponse(String productId, String warehouseId, Integer availableQuantity,
                                    Integer reservedQuantity, Integer damagedQuantity) {
}
