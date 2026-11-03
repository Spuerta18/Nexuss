package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;

/** The invoice issued when an order was paid, with the order total. */
public record InvoiceResponse(String identifier, String orderId, String buyerId, MoneyResponse totalAmount,
                              LocalDateTime issuedAt) {
}
