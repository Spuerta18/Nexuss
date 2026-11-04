package nexussMarket.domain.models.reports;

/** Units held in one warehouse, summed over all its inventory records. */
public record WarehouseStock(String warehouseId, String warehouseName, boolean active, int products,
                             long availableUnits, long reservedUnits, long damagedUnits) {
}
