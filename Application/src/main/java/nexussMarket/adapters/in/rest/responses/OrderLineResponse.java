package nexussMarket.adapters.in.rest.responses;

/** One confirmed order line, with the name and price frozen at confirmation. */
public record OrderLineResponse(String productId, String productName, Integer quantity, MoneyResponse unitPrice,
                                String warehouseId) {
}
