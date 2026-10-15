package nexussMarket.adapters.in.rest.responses;

import java.time.LocalDateTime;

/** A shipment and the timestamps of the steps it went through ({@code null} until reached). */
public record ShipmentResponse(String identifier, String orderId, String status, LocalDateTime createdAt,
                               LocalDateTime packedAt, LocalDateTime dispatchedAt, LocalDateTime deliveredAt) {
}
