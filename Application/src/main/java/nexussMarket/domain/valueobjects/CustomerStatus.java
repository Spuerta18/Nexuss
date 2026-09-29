package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Commercial condition of a {@link nexussMarket.domain.models.Buyer} with
 * respect to making purchases. Independent from {@link UserStatus}.
 */
public final class CustomerStatus extends DomainCatalog {

    public static final CustomerStatus ENABLED =
            new CustomerStatus("ENABLED", "Enabled", "The buyer may place orders normally.");
    public static final CustomerStatus SUSPENDED =
            new CustomerStatus("SUSPENDED", "Suspended", "The buyer is temporarily restricted from purchasing.");

    private CustomerStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code CustomerStatus} value list. */
    public static List<CustomerStatus> values() {
        return List.of(ENABLED, SUSPENDED);
    }

    /** Resolves a {@code CustomerStatus} from its code, or {@code null} if unknown. */
    public static CustomerStatus fromCode(String code) {
        return fromCode(values(), code);
    }
}
