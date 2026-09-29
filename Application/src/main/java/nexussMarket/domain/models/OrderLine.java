package nexussMarket.domain.models;

import java.math.BigDecimal;

/**
 * A single confirmed product line within an order, fixing the product name,
 * quantity and unit price at the time of purchase. Quantity must be greater
 * than zero.
 */
public class OrderLine {

    private Product product;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Warehouse warehouse;

    public OrderLine(Product product, String productName, int quantity, BigDecimal unitPrice, Warehouse warehouse) {
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

    public void setProduct(Product product) {
        this.product = product;
    }

    /** Product name at the time the order was confirmed. */
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    /** Quantity purchased. Must be greater than zero. */
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = requirePositive(quantity);
    }

    /** Price per unit at the time the order was confirmed. */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /** Warehouse the stock for this line was reserved from. */
    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}
