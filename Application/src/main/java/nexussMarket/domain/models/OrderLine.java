package nexussMarket.domain.models;

import java.math.BigDecimal;

/**
 * A single confirmed product line within an order, fixing the quantity and
 * unit price at the time of purchase. Quantity must be greater than zero.
 */
public class OrderLine {

    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;

    public OrderLine(Product product, int quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = requirePositive(quantity);
        this.unitPrice = unitPrice;
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
}