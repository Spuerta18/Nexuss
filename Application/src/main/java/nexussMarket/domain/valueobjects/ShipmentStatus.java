package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Current stage of a {@link nexussMarket.domain.models.Shipment}. The
 * lifecycle is forward-only: {@code CREATED} → {@code PACKED} →
 * {@code DISPATCHED} → {@code DELIVERED}.
 */
public final class ShipmentStatus extends DomainCatalog {

    public static final ShipmentStatus CREATED =
            new ShipmentStatus("CREATED", "Created", "The shipment has been registered for the order.");
    public static final ShipmentStatus PACKED =
            new ShipmentStatus("PACKED", "Packed", "The goods have been packed and are ready to leave the warehouse.");
    public static final ShipmentStatus DISPATCHED =
            new ShipmentStatus("DISPATCHED", "Dispatched", "The shipment has left the warehouse.");
    public static final ShipmentStatus DELIVERED =
            new ShipmentStatus("DELIVERED", "Delivered", "The shipment has been delivered to the buyer. Final status.");

    private ShipmentStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code ShipmentStatus} value list, in lifecycle order. */
    public static List<ShipmentStatus> values() {
        return List.of(CREATED, PACKED, DISPATCHED, DELIVERED);
    }

    /** Resolves a {@code ShipmentStatus} from its code, or {@code null} if unknown. */
    public static ShipmentStatus fromCode(String code) {
        return fromCode(values(), code);
    }
}
