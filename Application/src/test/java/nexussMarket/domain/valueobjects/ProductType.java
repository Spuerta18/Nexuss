package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Whether a {@link nexussMarket.domain.models.Product} requires physical
 * logistics or is delivered electronically.
 */
public final class ProductType extends DomainCatalog {

    public static final ProductType PHYSICAL =
            new ProductType("PHYSICAL", "Physical", "Requires inventory tracking and shipping.");
    public static final ProductType DIGITAL =
            new ProductType("DIGITAL", "Digital", "Delivered immediately upon payment confirmation.");

    private ProductType(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code ProductType} value list. */
    public static List<ProductType> values() {
        return List.of(PHYSICAL, DIGITAL);
    }
}