package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A {@code Shipment}. The order is referenced by ID; an order has at most one
 * shipment, so {@code orderId} is unique. Step timestamps are absent until
 * the step happens.
 */
@Document(collection = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderId;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime packedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
}
