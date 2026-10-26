package nexussMarket.domain.models;

import nexussMarket.domain.exceptions.InvalidQuantityException;

/**
 * A single product selection, with quantity, inside a shopping cart. Quantity
 * must be greater than zero. It only changes through its {@link ShoppingCart}.
 */
public class CartLine {

    private final Product product;
    private Integer quantity;

    public CartLine(Product product, int quantity) {
        this.product = product;
        this.quantity = requirePositive(quantity);
    }

    private static int requirePositive(int value) {
        if (value <= 0) {
            throw new InvalidQuantityException("quantity must be greater than zero");
        }
        return value;
    }

    /** Selected product. */
    public Product getProduct() {
        return product;
    }

    /** Quantity selected. Must be greater than zero. */
    public Integer getQuantity() {
        return quantity;
    }

    /** Package-private: only {@link ShoppingCart} changes the quantity of its lines. */
    void setQuantity(Integer quantity) {
        this.quantity = requirePositive(quantity);
    }
}
