package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.valueobjects.ShipmentStatus;

/**
 * Converts {@link Shipment} to and from {@link ShipmentDocument}. The order is
 * stored as {@code orderId} and must be resolved by the caller.
 */
public final class ShipmentMapper {

    private ShipmentMapper() {
    }

    public static ShipmentDocument toDocument(Shipment shipment) {
        ShipmentDocument document = new ShipmentDocument();
        document.setId(shipment.getIdentifier());
        document.setOrderId(shipment.getOrder().getIdentifier());
        document.setStatus(shipment.getStatus().getCode());
        document.setCreatedAt(shipment.getCreatedAt());
        document.setPackedAt(shipment.getPackedAt());
        document.setDispatchedAt(shipment.getDispatchedAt());
        document.setDeliveredAt(shipment.getDeliveredAt());
        return document;
    }

    /** {@code order} is the order referenced by {@code orderId}. */
    public static Shipment toDomain(ShipmentDocument document, Order order) {
        return new Shipment(document.getId(), order,
                CatalogCodes.resolve(ShipmentStatus::fromCode, document.getStatus(), "ShipmentStatus"),
                document.getCreatedAt(), document.getPackedAt(), document.getDispatchedAt(),
                document.getDeliveredAt());
    }
}
