package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Decision state of a {@link nexussMarket.domain.models.ReturnRequest}. A
 * request starts {@code REQUESTED} and is decided once: {@code APPROVED} or
 * {@code REJECTED}, both final.
 */
public final class ReturnStatus extends DomainCatalog {

    public static final ReturnStatus REQUESTED =
            new ReturnStatus("REQUESTED", "Requested", "The buyer has requested the return; awaiting a decision.");
    public static final ReturnStatus APPROVED =
            new ReturnStatus("APPROVED", "Approved", "The return has been accepted. Final status.");
    public static final ReturnStatus REJECTED =
            new ReturnStatus("REJECTED", "Rejected", "The return has been refused. Final status.");

    private ReturnStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code ReturnStatus} value list. */
    public static List<ReturnStatus> values() {
        return List.of(REQUESTED, APPROVED, REJECTED);
    }

    /** Resolves a {@code ReturnStatus} from its code, or {@code null} if unknown. */
    public static ReturnStatus fromCode(String code) {
        return fromCode(values(), code);
    }
}
