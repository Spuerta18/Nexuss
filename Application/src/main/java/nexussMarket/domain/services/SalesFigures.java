package nexussMarket.domain.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.reports.ProductSales;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OrderStatus;

/**
 * Adds up order lines into sales figures for the reports: revenue per
 * currency and units and revenue per product. Only orders that were paid
 * ({@link #SOLD}) count as sales.
 */
final class SalesFigures {

    /** Statuses of an order whose payment was confirmed. */
    static final Set<OrderStatus> SOLD = Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final Map<Currency, BigDecimal> revenueByCurrency = new LinkedHashMap<>();
    private final Map<String, ProductTotals> byProduct = new LinkedHashMap<>();
    private long units;

    void add(OrderLine line) {
        Money subtotal = line.getSubtotal();
        revenueByCurrency.merge(subtotal.getCurrency(), subtotal.getAmount(), BigDecimal::add);
        String key = line.getProduct().getIdentifier() + "|" + subtotal.getCurrency().getCode();
        byProduct.computeIfAbsent(key, ignored -> new ProductTotals(line.getProduct().getIdentifier(),
                line.getProductName(), subtotal.getCurrency())).add(line.getQuantity(), subtotal.getAmount());
        units += line.getQuantity();
    }

    static boolean sold(Order order) {
        return SOLD.contains(order.getStatus());
    }

    long units() {
        return units;
    }

    List<Money> revenueByCurrency() {
        List<Money> revenue = new ArrayList<>();
        revenueByCurrency.forEach((currency, amount) -> revenue.add(Money.of(amount, currency)));
        return revenue;
    }

    /** Best sellers first (by units, then by product identifier). */
    List<ProductSales> byProduct() {
        return byProduct.values().stream()
                .map(totals -> new ProductSales(totals.productId, totals.productName, totals.units,
                        Money.of(totals.revenue, totals.currency)))
                .sorted(Comparator.comparingLong(ProductSales::unitsSold).reversed()
                        .thenComparing(ProductSales::productId))
                .toList();
    }

    private static final class ProductTotals {

        private final String productId;
        private final String productName;
        private final Currency currency;
        private long units;
        private BigDecimal revenue = BigDecimal.ZERO;

        ProductTotals(String productId, String productName, Currency currency) {
            this.productId = productId;
            this.productName = productName;
            this.currency = currency;
        }

        void add(int quantity, BigDecimal amount) {
            units += quantity;
            revenue = revenue.add(amount);
        }
    }
}
