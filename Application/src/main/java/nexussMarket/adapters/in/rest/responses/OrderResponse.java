package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;
import java.util.List;

/** An order, its lines and its total (sum of quantity × unit price, in the order currency). */
public record OrderResponse(String identifier, String buyerId, String status, LocalDateTime createdAt,
                            List<OrderLineResponse> lines, MoneyResponse total) {
}
