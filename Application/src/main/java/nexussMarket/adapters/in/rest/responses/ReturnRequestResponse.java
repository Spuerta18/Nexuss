package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;

/** A return request and its decision, if any. */
public record ReturnRequestResponse(String identifier, String orderId, String buyerId, String reason, String status,
                                    LocalDateTime requestedAt, LocalDateTime decidedAt, String decidedById) {
}
