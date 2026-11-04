package nexussMarket.domain.models;

import java.time.LocalDateTime;

import nexussMarket.domain.exceptions.RefundAlreadyProcessedException;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.RefundStatus;

/**
 * The money paid back to a buyer for a {@link ReturnRequest}. It is processed
 * exactly once: {@link #process()} moves it from {@code PENDING} to
 * {@code PROCESSED}.
 */
public class Refund {

    private final String identifier;
    private final ReturnRequest returnRequest;
    private final Money amount;
    private RefundStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private long version;

    /** A new refund, {@code PENDING} since now. */
    public Refund(String identifier, ReturnRequest returnRequest, Money amount) {
        this(identifier, returnRequest, amount, RefundStatus.PENDING, LocalDateTime.now(), null);
    }

    /** Rebuilds an existing refund, e.g. when loaded from persistence. */
    public Refund(String identifier, ReturnRequest returnRequest, Money amount, RefundStatus status,
                  LocalDateTime createdAt, LocalDateTime processedAt) {
        this.identifier = identifier;
        this.returnRequest = returnRequest;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    /** Unique identifier of the refund. */
    public String getIdentifier() {
        return identifier;
    }

    /** Return request being refunded. */
    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }

    /** Amount paid back to the buyer. */
    public Money getAmount() {
        return amount;
    }

    /** Current processing state. */
    public RefundStatus getStatus() {
        return status;
    }

    /** Date and time the refund was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Date and time the refund was processed, or {@code null} while {@code PENDING}. */
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    /** Pays the refund. Only allowed while {@code PENDING}. */
    public void process() {
        if (status != RefundStatus.PENDING) {
            throw new RefundAlreadyProcessedException("Refund " + identifier + " was already processed");
        }
        status = RefundStatus.PROCESSED;
        processedAt = LocalDateTime.now();
    }

    /**
     * Optimistic-locking version of the stored {@code refund}: the number of
     * times it has been saved. A save whose version is not the stored one
     * fails with {@code ConcurrentUpdateException}, so two concurrent changes
     * never overwrite each other. Unrelated to {@code authTokenVersion}.
     */
    public long getVersion() {
        return version;
    }

    /** Sets the stored version; only for persistence adapters, after loading or saving. */
    public void restoreVersion(long version) {
        if (version < 0) {
            throw new IllegalArgumentException("version must never be negative");
        }
        this.version = version;
    }
}
