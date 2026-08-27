package nexussMarket.domain.models;

/**
 * A single product selection, with quantity, inside a shopping cart. Quantity
 * must be greater than zero.
 */
public class CartLine {

    private Product product;
    private Integer quantity;

    public CartLine(Product product, int quantity) {
        this.product = product;
        this.quantity = requirePositive(quantity);
    }

    private static int requirePositive(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return value;
    }

    /** Selected product. */
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    /** Quantity selected. Must be greater than zero. */
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = requirePositive(quantity);
    }
}