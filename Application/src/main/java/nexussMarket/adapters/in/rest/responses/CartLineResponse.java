package nexussMarket.adapters.in.rest.responses;

/** One line of a cart, with the product's current price. */
public record CartLineResponse(String productId, String productName, Integer quantity, MoneyResponse unitPrice) {
}
