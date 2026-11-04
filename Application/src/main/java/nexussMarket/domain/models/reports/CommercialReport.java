package nexussMarket.domain.models.reports;

import java.util.List;
import java.util.Map;

import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ReportPeriod;

/**
 * Orders created in a period: how many, how many per status, and the sales of
 * those that were paid ({@code PAID}, {@code SHIPPED}, {@code DELIVERED}) per
 * currency and per product.
 */
public record CommercialReport(ReportPeriod period, long orderCount, Map<String, Long> ordersByStatus,
                               long soldOrderCount, List<Money> salesByCurrency, List<ProductSales> salesByProduct) {
}
