package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Bodies of the read-only report routes. Amounts are {@link MoneyResponse}s, one per currency. */
public final class ReportResponses {

    private ReportResponses() {
    }

    public record PeriodResponse(LocalDate from, LocalDate to) {
    }

    public record ProductSalesResponse(String productId, String productName, long unitsSold, MoneyResponse revenue) {
    }

    public record CommercialReportResponse(PeriodResponse period, long orderCount, Map<String, Long> ordersByStatus,
                                           long soldOrderCount, List<MoneyResponse> salesByCurrency,
                                           List<ProductSalesResponse> salesByProduct) {
    }

    public record WarehouseStockResponse(String warehouseId, String warehouseName, boolean active, int products,
                                         long availableUnits, long reservedUnits, long damagedUnits) {
    }

    public record LowStockItemResponse(String productId, String productName, String warehouseId, int availableUnits) {
    }

    public record InventoryReportResponse(int lowStockThreshold, List<WarehouseStockResponse> warehouses,
                                          List<LowStockItemResponse> lowStock) {
    }

    public record LogisticsReportResponse(PeriodResponse period, long shipmentCount,
                                          Map<String, Long> shipmentsByStatus) {
    }

    public record SellerPerformanceReportResponse(PeriodResponse period, String sellerId, String sellerName,
                                                  long soldOrderCount, long unitsSold,
                                                  List<MoneyResponse> revenueByCurrency,
                                                  List<ProductSalesResponse> salesByProduct, long pendingOrderCount,
                                                  long cancelledOrderCount) {
    }
}
