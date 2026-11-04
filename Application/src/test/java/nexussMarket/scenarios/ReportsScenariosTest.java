package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidReportPeriodException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.models.reports.CommercialReport;
import nexussMarket.domain.models.reports.InventoryReport;
import nexussMarket.domain.models.reports.LogisticsReport;
import nexussMarket.domain.models.reports.ProductSales;
import nexussMarket.domain.models.reports.SellerPerformanceReport;
import nexussMarket.domain.valueobjects.ReportPeriod;

/**
 * The read-only reports on the real services. The in-memory store is shared by
 * the tests of this context, so the platform-wide reports are checked by the
 * difference that the data created here makes, and the per-seller report with
 * a seller of its own.
 */
class ReportsScenariosTest extends MemoryScenarioSupport {

    private static final ReportPeriod TODAY = ReportPeriod.of(LocalDate.now(), LocalDate.now());

    @Test
    void theCommercialReportCountsOrdersPerStatusAndSalesOfPaidOrdersOnly() {
        Administrator admin = administrator();
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        CommercialReport before = administratorPort.commercialReport(admin, TODAY);

        paidOrder(buyer(), product, 2, admin);
        pendingOrder(buyer(), product, 1);
        Buyer canceller = buyer();
        buyerPort.cancelOrder(canceller, orderReference(pendingOrder(canceller, product, 3)));
        CommercialReport after = administratorPort.commercialReport(admin, TODAY);

        assertEquals(before.orderCount() + 3, after.orderCount());
        assertEquals(count(before, "PAID") + 1, count(after, "PAID"));
        assertEquals(count(before, "PENDING_PAYMENT") + 1, count(after, "PENDING_PAYMENT"));
        assertEquals(count(before, "CANCELLED") + 1, count(after, "CANCELLED"));
        ProductSales sales = after.salesByProduct().stream()
                .filter(line -> line.productId().equals(product.getIdentifier())).findFirst().orElseThrow();
        assertEquals(2, sales.unitsSold(), "only the paid order counts as a sale");
        assertEquals(0, sales.revenue().getAmount().compareTo(BigDecimal.valueOf(2000)));
    }

    @Test
    void aSellersPerformanceIsExactAndSellersOnlySeeTheirOwn() {
        Administrator admin = administrator();
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        paidOrder(buyer(), product, 2, admin);
        pendingOrder(buyer(), product, 1);
        Buyer canceller = buyer();
        buyerPort.cancelOrder(canceller, orderReference(pendingOrder(canceller, product, 1)));
        // Another seller's sale must not count.
        paidOrder(buyer(), otherSellersProduct(), 1, admin);

        SellerPerformanceReport report = administratorPort.sellerPerformanceReport(admin,
                new Seller(seller.getIdentifier(), null, null, null, null), TODAY);
        SellerPerformanceReport own = sellerPort.performanceReport(seller, TODAY);

        assertEquals(1, report.soldOrderCount());
        assertEquals(2, report.unitsSold());
        assertEquals(1, report.revenueByCurrency().size());
        assertEquals(0, report.revenueByCurrency().get(0).getAmount().compareTo(BigDecimal.valueOf(2000)));
        assertEquals(1, report.pendingOrderCount());
        assertEquals(1, report.cancelledOrderCount());
        assertEquals(report, own, "the seller's own report is the same one");
        assertThrows(EntityNotFoundException.class, () -> administratorPort.sellerPerformanceReport(admin,
                new Seller("NO-SUCH-SELLER", null, null, null, null), TODAY));
    }

    @Test
    void theInventoryReportShowsStockPerWarehouseAndLowStockOfProductsOnSale() {
        Seller seller = seller();
        Warehouse warehouse = warehouseOf(seller);
        Product scarce = stockedProduct(seller, warehouse, 3);
        Product plenty = stockedProduct(seller, warehouse, 50);
        Product gone = stockedProduct(seller, warehouse, 1);
        sellerPort.discontinueProduct(seller, reference(gone));

        InventoryReport report = administratorPort.inventoryReport(administrator(), 5);

        List<String> low = report.lowStock().stream().map(item -> item.productId()).toList();
        assertTrue(low.contains(scarce.getIdentifier()));
        assertFalse(low.contains(plenty.getIdentifier()));
        assertFalse(low.contains(gone.getIdentifier()), "a discontinued product is not restocked");
        var stock = report.warehouses().stream()
                .filter(line -> line.warehouseId().equals(warehouse.getIdentifier())).findFirst().orElseThrow();
        assertEquals(3, stock.products());
        assertEquals(54, stock.availableUnits());
        assertThrows(IllegalArgumentException.class, () -> administratorPort.inventoryReport(administrator(), -1));
    }

    @Test
    void theLogisticsReportCountsShipmentsPerStatus() {
        Administrator admin = administrator();
        LogisticsOperator operator = logisticsOperator();
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 10);
        LogisticsReport before = administratorPort.logisticsReport(admin, TODAY);

        packedShipment(buyer(), product, 1, admin, operator);
        LogisticsReport after = administratorPort.logisticsReport(admin, TODAY);

        assertEquals(before.shipmentCount() + 1, after.shipmentCount());
        assertEquals(before.shipmentsByStatus().getOrDefault("PACKED", 0L) + 1,
                after.shipmentsByStatus().get("PACKED"));
    }

    @Test
    void periodsAreValidatedAndOrdersOutsideThemAreLeftOut() {
        assertThrows(InvalidReportPeriodException.class,
                () -> ReportPeriod.of(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)));
        assertThrows(InvalidReportPeriodException.class,
                () -> ReportPeriod.of(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)));
        assertEquals(366, ReportPeriod.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)).to().getDayOfYear());
        ReportPeriod defaults = ReportPeriod.of(null, null);
        assertEquals(LocalDate.now(), defaults.to());
        assertEquals(LocalDate.now().minusDays(29), defaults.from());

        Seller seller = seller();
        pendingOrder(buyer(), stockedProduct(seller, warehouseOf(seller), 5), 1);
        assertEquals(0, administratorPort.commercialReport(administrator(),
                ReportPeriod.of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))).orderCount());
    }

    @Test
    void onlyStaffSeeThePlatformReports() {
        assertThrows(OperationNotAllowedException.class, () -> administratorPort.commercialReport(seller(), TODAY));
        assertThrows(OperationNotAllowedException.class, () -> administratorPort.inventoryReport(buyer(), 5));
        assertThrows(OperationNotAllowedException.class, () -> sellerPort.performanceReport(buyer(), TODAY));
    }

    private Product otherSellersProduct() {
        Seller other = seller();
        return stockedProduct(other, warehouseOf(other), 5);
    }

    private static long count(CommercialReport report, String status) {
        return report.ordersByStatus().getOrDefault(status, 0L);
    }
}
