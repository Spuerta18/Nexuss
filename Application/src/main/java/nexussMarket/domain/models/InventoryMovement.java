package nexussMarket.domain.models;

import java.time.LocalDateTime;

import nexussMarket.domain.valueobjects.InventoryMovementType;

/**
 * A traceable stock movement applied to the {@link InventoryItem} identified by
 * {@code product} + {@code warehouse}. Movements are append-only: once
 * recorded they are never modified.
 */
public class InventoryMovement {

    private final Product product;
    private final Warehouse warehouse;
    private final InventoryMovementType type;
    private final int quantity;
    private final LocalDateTime occurredAt;

    public InventoryMovement(Product product, Warehouse warehouse, InventoryMovementType type, int quantity,
                             LocalDateTime occurredAt) {
        this.product = product;
        this.warehouse = warehouse;
        this.type = type;
        this.quantity = quantity;
        this.occurredAt = occurredAt;
    }

    /** Records a movement of {@code quantity} units on {@code item}, happening now. */
    public static InventoryMovement of(InventoryItem item, InventoryMovementType type, int quantity) {
        return new InventoryMovement(item.getProduct(), item.getWarehouse(), type, quantity, LocalDateTime.now());
    }

    /** Product of the affected inventory item. */
    public Product getProduct() {
        return product;
    }

    /** Warehouse of the affected inventory item. */
    public Warehouse getWarehouse() {
        return warehouse;
    }

    /** Kind of movement applied. */
    public InventoryMovementType getType() {
        return type;
    }

    /** Units moved; negative for downward adjustments, damaged units and released reservations. */
    public int getQuantity() {
        return quantity;
    }

    /** Date and time the movement happened. */
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
