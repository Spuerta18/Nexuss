package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Operational condition of a {@link nexussMarket.domain.models.Seller} within
 * the marketplace.
 */
public final class SellerStatus extends DomainCatalog {

    public static final SellerStatus ACTIVE =
            new SellerStatus("ACTIVE", "Active", "The seller may publish and sell products.");
    public static final SellerStatus SUSPENDED =
            new SellerStatus("SUSPENDED", "Suspended", "The seller is temporarily restricted from selling.");

    private SellerStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code SellerStatus} value list. */
    public static List<SellerStatus> values() {
        return List.of(ACTIVE, SUSPENDED);
    }
}