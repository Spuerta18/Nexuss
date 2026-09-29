package nexussMarket.domain.models;

import java.time.LocalDateTime;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.valueobjects.ReturnStatus;

/**
 * A buyer's request to return an {@link Order}. It is decided exactly once:
 * {@link #approve(User)} or {@link #reject(User)} moves it from
 * {@code REQUESTED} to a final status and records who decided and when.
 */
public class ReturnRequest {

    private final String identifier;
    private final Order order;
    private final Buyer buyer;
    private final String reason;
    private ReturnStatus status;
    private final LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private User decidedBy;

    /** A new return request, {@code REQUESTED} now. */
    public ReturnRequest(String identifier, Order order, Buyer buyer, String reason) {
        this(identifier, order, buyer, reason, ReturnStatus.REQUESTED, LocalDateTime.now(), null, null);
    }

    /** Rebuilds an existing return request, e.g. when loaded from persistence. */
    public ReturnRequest(String identifier, Order order, Buyer buyer, String reason, ReturnStatus status,
                         LocalDateTime requestedAt, LocalDateTime decidedAt, User decidedBy) {
        this.identifier = identifier;
        this.order = order;
        this.buyer = buyer;
        this.reason = reason;
        this.status = status;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
    }

    /** Unique identifier of the return request. */
    public String getIdentifier() {
        return identifier;
    }

    /** Order the buyer wants to return. */
    public Order getOrder() {
        return order;
    }

    /** Buyer who requested the return. */
    public Buyer getBuyer() {
        return buyer;
    }

    /** Reason given by the buyer. */
    public String getReason() {
        return reason;
    }

    /** Current decision state. */
    public ReturnStatus getStatus() {
        return status;
    }

    /** Date and time the return was requested. */
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    /** Date and time the request was decided, or {@code null} while {@code REQUESTED}. */
    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    /** User who decided the request, or {@code null} while {@code REQUESTED}. */
    public User getDecidedBy() {
        return decidedBy;
    }

    /** Accepts the return. Only allowed while {@code REQUESTED}. */
    public void approve(User decider) {
        decide(ReturnStatus.APPROVED, decider);
    }

    /** Refuses the return. Only allowed while {@code REQUESTED}. */
    public void reject(User decider) {
        decide(ReturnStatus.REJECTED, decider);
    }

    private void decide(ReturnStatus decision, User decider) {
        if (status != ReturnStatus.REQUESTED) {
            throw new BusinessRuleViolationException("Return request " + identifier + " was already decided: "
                    + status.getCode());
        }
        status = decision;
        decidedAt = LocalDateTime.now();
        decidedBy = decider;
    }
}
