package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Unique business role assigned to a {@link nexussMarket.domain.models.User}.
 */
public final class SystemRole extends DomainCatalog {

    public static final SystemRole BUYER =
            new SystemRole("BUYER", "Buyer", "Purchases products published in the marketplace.");
    public static final SystemRole SELLER =
            new SystemRole("SELLER", "Seller", "Registers and manages products for sale.");
    public static final SystemRole LOGISTICS_OPERATOR =
            new SystemRole("LOGISTICS_OPERATOR", "Logistics Operator", "Operates warehouses and manages dispatches.");
    public static final SystemRole ADMINISTRATOR =
            new SystemRole("ADMINISTRATOR", "Administrator", "Manages sellers and Marketplace-owned warehouses.");
    public static final SystemRole SUPERVISOR =
            new SystemRole("SUPERVISOR", "Supervisor", "Read-only operational monitoring role.");

    private SystemRole(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns all allowed {@code SystemRole} values. */
    public static List<SystemRole> values() {
        return List.of(BUYER, SELLER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR);
    }

    /** Resolves a {@code SystemRole} from its code, or {@code null} if unknown. */
    public static SystemRole fromCode(String code) {
        return values().stream()
                .filter(r -> r.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}