package nexussMarket.domain.models.reports;

import nexussMarket.domain.valueobjects.Money;

/** Units sold and revenue of one product, in one currency, as recorded in the order lines. */
public record ProductSales(String productId, String productName, long unitsSold, Money revenue) {
}
