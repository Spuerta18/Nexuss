package nexussMarket.adapters.in.rest.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.models.reports.CommercialReport;
import nexussMarket.domain.models.reports.InventoryReport;
import nexussMarket.domain.models.reports.LowStockItem;
import nexussMarket.domain.models.reports.ProductSales;
import nexussMarket.domain.models.reports.SellerPerformanceReport;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ReportPeriod;

/** The report routes: parameters, body shape, period validation and who may read them. */
class ReportsRestControllerTest extends RestControllerTestSupport {

    private static final ReportPeriod NOVEMBER = new ReportPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    private static final Money SALES = Money.of(BigDecimal.valueOf(90000), Currency.COP);

    @Test
    void theCommercialReportTakesTheRequestedPeriod() throws Exception {
        when(administratorPort.commercialReport(any(), eq(NOVEMBER))).thenReturn(new CommercialReport(NOVEMBER, 3,
                Map.of("PAID", 2L, "CANCELLED", 1L), 2, List.of(SALES),
                List.of(new ProductSales("PRD-1", "Mouse", 2, SALES))));

        mockMvc.perform(get("/api/admin/reports/commercial?from=2026-11-01&to=2026-11-30")
                        .header(authorization(), bearer(administrator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.from").value("2026-11-01"))
                .andExpect(jsonPath("$.orderCount").value(3))
                .andExpect(jsonPath("$.ordersByStatus.PAID").value(2))
                .andExpect(jsonPath("$.salesByCurrency[0].currency").value("COP"))
                .andExpect(jsonPath("$.salesByProduct[0].unitsSold").value(2));
    }

    @Test
    void theInventoryReportTakesTheThreshold() throws Exception {
        when(supervisorPort.inventoryReport(any(), eq(3))).thenReturn(new InventoryReport(3, List.of(),
                List.of(new LowStockItem("PRD-1", "Mouse", "WH-1", 2))));

        mockMvc.perform(get("/api/supervisor/reports/inventory?lowStockThreshold=3")
                        .header(authorization(), bearer(supervisor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockThreshold").value(3))
                .andExpect(jsonPath("$.lowStock[0].availableUnits").value(2));
    }

    @Test
    void aSellerReadsTheirOwnPerformanceAndStaffNameTheSeller() throws Exception {
        SellerPerformanceReport report = new SellerPerformanceReport(NOVEMBER, "S-1", "Sam Seller", 2, 3,
                List.of(SALES), List.of(), 1, 0);
        when(sellerPort.performanceReport(any(), eq(NOVEMBER))).thenReturn(report);
        when(administratorPort.sellerPerformanceReport(any(), argThat(seller -> "S-1".equals(seller.getIdentifier())),
                eq(NOVEMBER))).thenReturn(report);

        mockMvc.perform(get("/api/seller/reports/performance?from=2026-11-01&to=2026-11-30")
                        .header(authorization(), bearer(seller())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value("S-1"))
                .andExpect(jsonPath("$.unitsSold").value(3));
        mockMvc.perform(get("/api/admin/reports/sellers/S-1?from=2026-11-01&to=2026-11-30")
                        .header(authorization(), bearer(administrator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soldOrderCount").value(2));
    }

    @Test
    void invalidPeriodsAre400() throws Exception {
        String admin = bearer(administrator());
        mockMvc.perform(get("/api/admin/reports/commercial?from=2026-11-30&to=2026-11-01").header(authorization(), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/reports/logistics?from=2025-01-01&to=2026-06-01").header(authorization(), admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/reports/commercial?from=yesterday").header(authorization(), admin))
                .andExpect(status().isBadRequest());
        verify(administratorPort, never()).commercialReport(any(), any());
        verify(administratorPort, never()).logisticsReport(any(), any());
    }

    @Test
    void reportsAreForbiddenToOtherRoles() throws Exception {
        mockMvc.perform(get("/api/admin/reports/commercial").header(authorization(), bearer(seller())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/supervisor/reports/logistics").header(authorization(), bearer(logisticsOperator())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/seller/reports/performance").header(authorization(), bearer(buyer())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reports/inventory"))
                .andExpect(status().isUnauthorized());
    }
}
