package nexussMarket.domain.models;

import java.util.ArrayList;
import java.util.List;

/**
 * A buyer's provisional selection of products prior to confirming an order. A
 * cart has no binding commercial commitment and is converted into an
 * {@link Order} upon buyer confirmation, after which it does not persist.
 */
public class ShoppingCart {

    private String identifier;
    private Buyer buyer;
    private List<CartLine> lines = new ArrayList<>();

    public ShoppingCart(String identifier, Buyer buyer) {
        this.identifier = identifier;
        this.buyer = buyer;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /** Owner of the cart. */
    public Buyer getBuyer() {
        return buyer;
    }

    public void setBuyer(Buyer buyer) {
        this.buyer = buyer;
    }

    /** Products and quantities currently selected. */
    public List<CartLine> getLines() {
        return lines;
    }

    public void setLines(List<CartLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}