package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;

/** A refund for a return request. */
public record RefundResponse(String identifier, String returnRequestId, MoneyResponse amount, String status,
                             LocalDateTime createdAt, LocalDateTime processedAt) {
}
