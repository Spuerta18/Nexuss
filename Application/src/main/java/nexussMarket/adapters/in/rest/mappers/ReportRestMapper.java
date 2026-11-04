package nexussMarket.adapters.in.rest.mappers;

import java.time.LocalDate;
import java.util.List;

import nexussMarket.adapters.in.rest.responses.ReportResponses.CommercialReportResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.InventoryReportResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.LogisticsReportResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.LowStockItemResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.PeriodResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.ProductSalesResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.SellerPerformanceReportResponse;
import nexussMarket.adapters.in.rest.responses.ReportResponses.WarehouseStockResponse;
import nexussMarket.domain.models.reports.CommercialReport;
import nexussMarket.domain.models.reports.InventoryReport;
import nexussMarket.domain.models.reports.LogisticsReport;
import nexussMarket.domain.models.reports.ProductSales;
import nexussMarket.domain.models.reports.SellerPerformanceReport;
import nexussMarket.domain.valueobjects.ReportPeriod;

/** Report parameters in, report bodies out. */
public final class ReportRestMapper {

    private ReportRestMapper() {
    }

    /** {@code from} / {@code to} as ISO dates ({@code 2026-11-01}); missing ones take the defaults of ReportPeriod. */
    public static ReportPeriod period(LocalDate from, LocalDate to) {
        return ReportPeriod.of(from, to);
    }

    public static CommercialReportResponse toResponse(CommercialReport report) {
        return new CommercialReportResponse(toResponse(report.period()), report.orderCount(), report.ordersByStatus(),
                report.soldOrderCount(), report.salesByCurrency().stream().map(CatalogRestMapper::toResponse).toList(),
                toProductSales(report.salesByProduct()));
    }

    public static InventoryReportResponse toResponse(InventoryReport report) {
        return new InventoryReportResponse(report.lowStockThreshold(),
                report.warehouses().stream().map(stock -> new WarehouseStockResponse(stock.warehouseId(),
                        stock.warehouseName(), stock.active(), stock.products(), stock.availableUnits(),
                        stock.reservedUnits(), stock.damagedUnits())).toList(),
                report.lowStock().stream().map(item -> new LowStockItemResponse(item.productId(), item.productName(),
                        item.warehouseId(), item.availableUnits())).toList());
    }

    public static LogisticsReportResponse toResponse(LogisticsReport report) {
        return new LogisticsReportResponse(toResponse(report.period()), report.shipmentCount(),
                report.shipmentsByStatus());
    }

    public static SellerPerformanceReportResponse toResponse(SellerPerformanceReport report) {
        return new SellerPerformanceReportResponse(toResponse(report.period()), report.sellerId(), report.sellerName(),
                report.soldOrderCount(), report.unitsSold(),
                report.revenueByCurrency().stream().map(CatalogRestMapper::toResponse).toList(),
                toProductSales(report.salesByProduct()), report.pendingOrderCount(), report.cancelledOrderCount());
    }

    private static PeriodResponse toResponse(ReportPeriod period) {
        return new PeriodResponse(period.from(), period.to());
    }

    private static List<ProductSalesResponse> toProductSales(List<ProductSales> sales) {
        return sales.stream().map(product -> new ProductSalesResponse(product.productId(), product.productName(),
                product.unitsSold(), CatalogRestMapper.toResponse(product.revenue()))).toList();
    }
}
