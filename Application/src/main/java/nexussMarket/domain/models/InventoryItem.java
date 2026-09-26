package nexussMarket.domain.models;

/**
 * Distributed stock of a specific product held in a specific warehouse. Every
 * inventory record is linked to exactly one {@link Product} and one
 * {@link Warehouse}. Negative stock is never permitted.
 */
public class InventoryItem {

    private Product product;
    private Warehouse warehouse;
    private Integer availableQuantity;
    private Integer reservedQuantity;

    public InventoryItem(Product product, Warehouse warehouse, int availableQuantity, int reservedQuantity) {
        this.product = product;
        this.warehouse = warehouse;
        this.availableQuantity = requireNonNegative(availableQuantity, "availableQuantity");
        this.reservedQuantity = requireNonNegative(reservedQuantity, "reservedQuantity");
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must never be negative");
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

    /** Reserves stock for a pending order when the available quantity suffices. */
    public void reserve(int amount) {
        requireNonNegative(amount, "amount");
        if (amount > availableQuantity) {
            throw new IllegalStateException("Cannot reserve more units than the available quantity");
        }
        availableQuantity -= amount;
        reservedQuantity += amount;
    }
}