package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Processing state of a {@link nexussMarket.domain.models.Refund}: it starts
 * {@code PENDING} and is processed once, becoming {@code PROCESSED} (final).
 */
public final class RefundStatus extends DomainCatalog {

    public static final RefundStatus PENDING =
            new RefundStatus("PENDING", "Pending", "The refund has been created and awaits processing.");
    public static final RefundStatus PROCESSED =
            new RefundStatus("PROCESSED", "Processed", "The refund has been paid back to the buyer. Final status.");

    private RefundStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code RefundStatus} value list. */
    public static List<RefundStatus> values() {
        return List.of(PENDING, PROCESSED);
    }

    /** Resolves a {@code RefundStatus} from its code, or {@code null} if unknown. */
    public static RefundStatus fromCode(String code) {
        return fromCode(values(), code);
    }
}
