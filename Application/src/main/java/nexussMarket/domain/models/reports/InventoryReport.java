package nexussMarket.domain.models.reports;

import java.util.List;

/** Current stock (a snapshot, not a period): per warehouse, and the records running low. */
public record InventoryReport(int lowStockThreshold, List<WarehouseStock> warehouses, List<LowStockItem> lowStock) {
}
