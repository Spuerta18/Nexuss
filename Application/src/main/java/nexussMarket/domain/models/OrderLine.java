package nexussMarket.domain.models;

import nexussMarket.domain.valueobjects.Money;

/**
 * A single confirmed product line within an order, fixing the product name,
 * quantity and unit price at the time of purchase. Quantity must be greater
 * than zero. Immutable once created.
 */
public class OrderLine {

    private final Product product;
    private final String productName;
    private final Integer quantity;
    private final Money unitPrice;
    private final Warehouse warehouse;

    public OrderLine(Product product, String productName, int quantity, Money unitPrice, Warehouse warehouse) {
        this.product = product;
        this.productName = productName;
        this.quantity = requirePositive(quantity);
        this.unitPrice = unitPrice;
        this.warehouse = warehouse;
    }

    private static int requirePositive(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return value;
    }

    /** Purchased product. */
    public Product getProduct() {
        return product;
    }

    /** Product name at the time the order was confirmed. */
    public String getProductName() {
        return productName;
    }

    /** Quantity purchased. Must be greater than zero. */
    public Integer getQuantity() {
        return quantity;
    }

    /** Price per unit at the time the order was confirmed. */
    public Money getUnitPrice() {
        return unitPrice;
    }

    /** Warehouse the stock for this line was reserved from. */
    public Warehouse getWarehouse() {
        return warehouse;
    }
}
