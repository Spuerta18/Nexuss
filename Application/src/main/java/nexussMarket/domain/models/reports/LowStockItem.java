package nexussMarket.domain.models.reports;

/** An inventory record of a product still on sale whose available stock is at or below the threshold. */
public record LowStockItem(String productId, String productName, String warehouseId, int availableUnits) {
}
