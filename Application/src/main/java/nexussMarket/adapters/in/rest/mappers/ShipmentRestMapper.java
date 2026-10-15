package nexussMarket.adapters.in.rest.mappers;

import nexussMarket.adapters.in.rest.requests.CreateShipmentRequest;
import nexussMarket.adapters.in.rest.responses.ShipmentResponse;
import nexussMarket.domain.models.Shipment;

/** Converts shipment requests into domain models and shipments into {@link ShipmentResponse}. */
public final class ShipmentRestMapper {

    private ShipmentRestMapper() {
    }

    /** A new shipment for the referenced order; the service checks the order and saves it. */
    public static Shipment newShipment(CreateShipmentRequest request) {
        return new Shipment(request.shipmentId(), DomainReferences.order(request.orderId()));
    }

    public static ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(shipment.getIdentifier(),
                shipment.getOrder() != null ? shipment.getOrder().getIdentifier() : null,
                RestCodes.code(shipment.getStatus()), shipment.getCreatedAt(), shipment.getPackedAt(),
                shipment.getDispatchedAt(), shipment.getDeliveredAt());
    }
}
