package nexussMarket.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.valueobjects.OrderStatus;

/**
 * The formal commercial commitment between a buyer and one or more sellers.
 * Its lifecycle is the central process of the system and follows a strict,
 * forward-only sequence; once {@code DELIVERED} it can never be modified.
 */
public class Order {

    private String identifier;
    private Buyer buyer;
    private List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status;
    private LocalDateTime createdAt;

    public Order(String identifier, Buyer buyer, OrderStatus status) {
        this.identifier = identifier;
        this.buyer = buyer;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    /** Unique identifier of the order. */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /** Buyer who placed the order. */
    public Buyer getBuyer() {
        return buyer;
    }

    public void setBuyer(Buyer buyer) {
        this.buyer = buyer;
    }

    /** Products, quantities, and unit prices confirmed for this order. */
    public List<OrderLine> getLines() {
        return lines;
    }

    public void setLines(List<OrderLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    /** Current state of the order lifecycle. */
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /** Date and time the order was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}