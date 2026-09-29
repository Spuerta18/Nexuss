package nexussMarket.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The invoice issued to a {@link Buyer} for an {@link Order}. A simple record:
 * it is issued once and has no lifecycle, so it is immutable.
 */
public class Invoice {

    private final String identifier;
    private final Order order;
    private final Buyer buyer;
    private final BigDecimal totalAmount;
    private final LocalDateTime issuedAt;

    public Invoice(String identifier, Order order, Buyer buyer, BigDecimal totalAmount, LocalDateTime issuedAt) {
        this.identifier = identifier;
        this.order = order;
        this.buyer = buyer;
        this.totalAmount = totalAmount;
        this.issuedAt = issuedAt;
    }

    /** Unique identifier of the invoice. */
    public String getIdentifier() {
        return identifier;
    }

    /** Order being invoiced. */
    public Order getOrder() {
        return order;
    }

    /** Buyer the invoice is issued to. */
    public Buyer getBuyer() {
        return buyer;
    }

    /** Total amount charged. */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /** Date and time the invoice was issued. */
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
}
