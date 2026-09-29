package nexussMarket.domain.models;

import nexussMarket.domain.exceptions.InsufficientStockException;

/**
 * Distributed stock of a specific product held in a specific warehouse. Every
 * inventory record is linked to exactly one {@link Product} and one
 * {@link Warehouse}. Negative stock is never permitted.
 *
 * <p>Damaged units are tracked apart from {@code availableQuantity}, so they
 * can never be reserved.</p>
 */
public class InventoryItem {

    private Product product;
    private Warehouse warehouse;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer damagedQuantity;

    public InventoryItem(Product product, Warehouse warehouse, int availableQuantity, int reservedQuantity,
                         int damagedQuantity) {
        this.product = product;
        this.warehouse = warehouse;
        this.availableQuantity = requireNonNegative(availableQuantity, "availableQuantity");
        this.reservedQuantity = requireNonNegative(reservedQuantity, "reservedQuantity");
        this.damagedQuantity = requireNonNegative(damagedQuantity, "damagedQuantity");
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must never be negative");
        }
        return value;
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    /** Product this inventory record belongs to. */
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    /** Warehouse holding the stock. */
    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    /** Quantity available for sale. Must never be negative. */
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = requireNonNegative(availableQuantity, "availableQuantity");
    }

    /** Quantity reserved by open orders. Must never be negative. */
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = requireNonNegative(reservedQuantity, "reservedQuantity");
    }

    /** Quantity marked as damaged. Never available for reservation. Must never be negative. */
    public Integer getDamagedQuantity() {
        return damagedQuantity;
    }

    public void setDamagedQuantity(Integer damagedQuantity) {
        this.damagedQuantity = requireNonNegative(damagedQuantity, "damagedQuantity");
    }

    /** Adds received or returned units to the available stock. */
    public void receive(int amount) {
        availableQuantity += requirePositive(amount, "amount");
    }

    /** Reserves stock for a pending order when the available quantity suffices. */
    public void reserve(int amount) {
        requirePositive(amount, "amount");
        if (amount > availableQuantity) {
            throw new InsufficientStockException("Cannot reserve " + amount + " units of product "
                    + product.getIdentifier() + ": only " + availableQuantity + " available");
        }
        availableQuantity -= amount;
        reservedQuantity += amount;
    }

    /** Returns previously reserved units to the available stock. */
    public void releaseReservation(int amount) {
        requireReserved(amount, "release");
        reservedQuantity -= amount;
        availableQuantity += amount;
    }

    /** Removes previously reserved units from the warehouse because they were sold and dispatched. */
    public void confirmOutbound(int amount) {
        requireReserved(amount, "confirm outbound of");
        reservedQuantity -= amount;
    }

    /** Applies a manual correction to the available stock; the result can never be negative. */
    public void adjust(int quantityDelta) {
        if (availableQuantity + quantityDelta < 0) {
            throw new InsufficientStockException("Adjustment of " + quantityDelta
                    + " would leave negative stock for product " + product.getIdentifier());
        }
        availableQuantity += quantityDelta;
    }

    /** Moves available units to damaged, so they can no longer be reserved. */
    public void markDamaged(int amount) {
        requirePositive(amount, "amount");
        if (amount > availableQuantity) {
            throw new InsufficientStockException("Cannot mark " + amount + " units of product "
                    + product.getIdentifier() + " as damaged: only " + availableQuantity + " available");
        }
        availableQuantity -= amount;
        damagedQuantity += amount;
    }

    private void requireReserved(int amount, String action) {
        requirePositive(amount, "amount");
        if (amount > reservedQuantity) {
            throw new InsufficientStockException("Cannot " + action + " " + amount + " units of product "
                    + product.getIdentifier() + ": only " + reservedQuantity + " reserved");
        }
    }
}
