package nexussMarket.domain.models.reports;

import java.util.List;

import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ReportPeriod;

/**
 * One seller's results in a period, from the lines of their products in the
 * orders created then: paid orders, units sold and revenue (per currency and
 * per product), plus the orders still pending payment and those cancelled.
 */
public record SellerPerformanceReport(ReportPeriod period, String sellerId, String sellerName, long soldOrderCount,
                                      long unitsSold, List<Money> revenueByCurrency, List<ProductSales> salesByProduct,
                                      long pendingOrderCount, long cancelledOrderCount) {
}
