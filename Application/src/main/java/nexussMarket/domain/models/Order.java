package nexussMarket.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.valueobjects.OrderStatus;

/**
 * The formal commercial commitment between a buyer and one or more sellers.
 * Its lifecycle is the central process of the system and follows a strict,
 * forward-only sequence; once {@code DELIVERED} it can never be modified.
 *
 * <p>The status can only change through {@link #advanceTo(OrderStatus)} and
 * {@link #cancel()}, so the lifecycle rules cannot be bypassed.</p>
 */
public class Order {

    private static final List<OrderStatus> LIFECYCLE = List.of(
            OrderStatus.CART, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

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

    /** Date and time the order was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Moves the order exactly one step forward in the lifecycle. Fails if the
     * order is {@code DELIVERED} or {@code CANCELLED}, or if {@code target}
     * is not the immediate next status.
     */
    public void advanceTo(OrderStatus target) {
        requireNotFinalized();
        if (target == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + identifier + " cannot be cancelled by advancing its status; cancel it instead");
        }
        int currentIndex = LIFECYCLE.indexOf(status);
        int targetIndex = LIFECYCLE.indexOf(target);
        if (targetIndex != currentIndex + 1) {
            throw new InvalidOrderStatusTransitionException("Cannot transition order " + identifier
                    + " from " + status.getCode() + " to " + target.getCode());
        }
        status = target;
    }

    /** Cancels the order. Only allowed before payment is confirmed. */
    public void cancel() {
        requireNotFinalized();
        if (status != OrderStatus.CART && status != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + identifier + " cannot be cancelled from status " + status.getCode());
        }
        status = OrderStatus.CANCELLED;
    }

    private void requireNotFinalized() {
        if (status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + identifier + " is DELIVERED and cannot be modified");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + identifier + " is CANCELLED and cannot re-enter the order lifecycle");
        }
    }
}
