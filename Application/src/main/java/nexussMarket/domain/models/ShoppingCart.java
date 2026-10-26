package nexussMarket.domain.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.CurrencyMismatchException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidQuantityException;
import nexussMarket.domain.exceptions.ProductNotPurchasableException;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.ProductStatus;

/**
 * A buyer's provisional selection of products prior to confirming an order. A
 * cart has no binding commercial commitment and is converted into an
 * {@link Order} upon buyer confirmation, after which it does not persist.
 *
 * <p>Lines only change through {@link #addLine}, {@link #updateLineQuantity}
 * and {@link #removeLine}. A cart holds at most one line per product, only
 * {@code PUBLISHED} products, and every product priced in the same
 * {@link Currency}.</p>
 */
public class ShoppingCart {

    private final String identifier;
    private final Buyer buyer;
    private final List<CartLine> lines;

    /** A reference or a new, empty cart. */
    public ShoppingCart(String identifier, Buyer buyer) {
        this(identifier, buyer, null);
    }

    /** A cart with its lines, e.g. loaded from persistence. */
    public ShoppingCart(String identifier, Buyer buyer, List<CartLine> lines) {
        this.identifier = identifier;
        this.buyer = buyer;
        this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public String getIdentifier() {
        return identifier;
    }

    /** Owner of the cart. */
    public Buyer getBuyer() {
        return buyer;
    }

    /** Products and quantities currently selected. Read-only: use the line methods to change them. */
    public List<CartLine> getLines() {
        return List.copyOf(lines);
    }

    /**
     * Adds {@code quantity} units of {@code product}. If the cart already has a
     * line for the product, the quantities are merged into that line.
     */
    public void addLine(Product product, int quantity) {
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new ProductNotPurchasableException(
                    "Product " + product.getIdentifier() + " is not available for purchase");
        }
        requireCartCurrency(product);
        Optional<CartLine> existingLine = findLine(product);
        try {
            if (existingLine.isPresent()) {
                existingLine.get().setQuantity(existingLine.get().getQuantity() + quantity);
            } else {
                lines.add(new CartLine(product, quantity));
            }
        } catch (InvalidQuantityException e) {
            throw invalidQuantity(product, e);
        }
    }

    /** Replaces the quantity of the line of {@code product}. */
    public void updateLineQuantity(Product product, int quantity) {
        CartLine line = findLine(product).orElseThrow(() -> lineNotFound(product));
        try {
            line.setQuantity(quantity);
        } catch (InvalidQuantityException e) {
            throw invalidQuantity(product, e);
        }
    }

    /** Removes the line of {@code product}. */
    public void removeLine(Product product) {
        if (!lines.removeIf(line -> isLineOf(line, product))) {
            throw lineNotFound(product);
        }
    }

    /** Every product in the cart is priced in the same currency; a product without price cannot be added. */
    private void requireCartCurrency(Product product) {
        if (product.getPrice() == null) {
            throw new ProductNotPurchasableException("Product " + product.getIdentifier() + " has no price");
        }
        Currency currency = product.getPrice().getCurrency();
        for (CartLine line : lines) {
            if (line.getProduct().getPrice() == null) {
                continue;
            }
            Currency lineCurrency = line.getProduct().getPrice().getCurrency();
            if (!lineCurrency.equals(currency)) {
                throw new CurrencyMismatchException("Cart " + identifier + " holds products priced in "
                        + lineCurrency.getCode() + "; product " + product.getIdentifier() + " is priced in "
                        + currency.getCode());
            }
        }
    }

    private Optional<CartLine> findLine(Product product) {
        return lines.stream().filter(line -> isLineOf(line, product)).findFirst();
    }

    private static boolean isLineOf(CartLine line, Product product) {
        return line.getProduct().getIdentifier().equals(product.getIdentifier());
    }

    private EntityNotFoundException lineNotFound(Product product) {
        return new EntityNotFoundException(
                "No cart line found for product " + product.getIdentifier() + " in cart " + identifier);
    }

    private InvalidQuantityException invalidQuantity(Product product, InvalidQuantityException cause) {
        return new InvalidQuantityException("Invalid quantity for product " + product.getIdentifier()
                + " in cart " + identifier + ": " + cause.getMessage());
    }
}
