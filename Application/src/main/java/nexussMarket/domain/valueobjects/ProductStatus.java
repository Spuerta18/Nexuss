package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Publication state of a {@link nexussMarket.domain.models.Product} in the
 * catalog.
 */
public final class ProductStatus extends DomainCatalog {

    public static final ProductStatus PUBLISHED =
            new ProductStatus("PUBLISHED", "Published", "Visible and purchasable in the public catalog.");
    public static final ProductStatus SUSPENDED =
            new ProductStatus("SUSPENDED", "Suspended", "Temporarily hidden from the catalog.");
    public static final ProductStatus DISCONTINUED =
            new ProductStatus("DISCONTINUED", "Discontinued", "Permanently removed from sale.");

    private ProductStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code ProductStatus} value list. */
    public static List<ProductStatus> values() {
        return List.of(PUBLISHED, SUSPENDED, DISCONTINUED);
    }
}