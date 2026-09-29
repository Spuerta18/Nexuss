package nexussMarket.domain.models;

import java.time.LocalDateTime;

import nexussMarket.domain.exceptions.InvalidShipmentStatusTransitionException;
import nexussMarket.domain.valueobjects.ShipmentStatus;

/**
 * The physical delivery of an {@link Order}. Its lifecycle is forward-only,
 * one step at a time: {@code CREATED} → {@code PACKED} → {@code DISPATCHED}
 * → {@code DELIVERED}.
 *
 * <p>The status and the step timestamps can only change through
 * {@link #pack()}, {@link #dispatch()} and {@link #confirmDelivery()}, so the
 * lifecycle cannot be bypassed. Each timestamp is {@code null} until its step
 * happens.</p>
 */
public class Shipment {

    private final String identifier;
    private final Order order;
    private ShipmentStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime packedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;

    /** A new shipment for {@code order}, {@code CREATED} now. */
    public Shipment(String identifier, Order order) {
        this(identifier, order, ShipmentStatus.CREATED, LocalDateTime.now(), null, null, null);
    }

    /** Rebuilds an existing shipment, e.g. when loaded from persistence. */
    public Shipment(String identifier, Order order, ShipmentStatus status, LocalDateTime createdAt,
                    LocalDateTime packedAt, LocalDateTime dispatchedAt, LocalDateTime deliveredAt) {
        this.identifier = identifier;
        this.order = order;
        this.status = status;
        this.createdAt = createdAt;
        this.packedAt = packedAt;
        this.dispatchedAt = dispatchedAt;
        this.deliveredAt = deliveredAt;
    }

    /** Unique identifier of the shipment. */
    public String getIdentifier() {
        return identifier;
    }

    /** Order being delivered. */
    public Order getOrder() {
        return order;
    }

    /** Current stage of the shipment. */
    public ShipmentStatus getStatus() {
        return status;
    }

    /** Date and time the shipment was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Date and time the goods were packed, or {@code null} if not packed yet. */
    public LocalDateTime getPackedAt() {
        return packedAt;
    }

    /** Date and time the shipment left the warehouse, or {@code null} if not dispatched yet. */
    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    /** Date and time the shipment was delivered, or {@code null} if not delivered yet. */
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    /** Marks the goods as packed. Only allowed from {@code CREATED}. */
    public void pack() {
        requireStatus(ShipmentStatus.CREATED, ShipmentStatus.PACKED);
        status = ShipmentStatus.PACKED;
        packedAt = LocalDateTime.now();
    }

    /** Marks the shipment as having left the warehouse. Only allowed from {@code PACKED}. */
    public void dispatch() {
        requireStatus(ShipmentStatus.PACKED, ShipmentStatus.DISPATCHED);
        status = ShipmentStatus.DISPATCHED;
        dispatchedAt = LocalDateTime.now();
    }

    /** Marks the shipment as delivered to the buyer. Only allowed from {@code DISPATCHED}. */
    public void confirmDelivery() {
        requireStatus(ShipmentStatus.DISPATCHED, ShipmentStatus.DELIVERED);
        status = ShipmentStatus.DELIVERED;
        deliveredAt = LocalDateTime.now();
    }

    private void requireStatus(ShipmentStatus required, ShipmentStatus target) {
        if (status != required) {
            throw new InvalidShipmentStatusTransitionException("Cannot transition shipment " + identifier
                    + " from " + status.getCode() + " to " + target.getCode());
        }
    }
}
