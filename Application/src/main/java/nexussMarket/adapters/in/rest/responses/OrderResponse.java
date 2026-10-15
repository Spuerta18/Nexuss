package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;
import java.util.List;

/** An order and its lines. */
public record OrderResponse(String identifier, String buyerId, String status, LocalDateTime createdAt,
                            List<OrderLineResponse> lines) {
}
