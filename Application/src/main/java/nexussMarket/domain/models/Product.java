package nexussMarket.domain.models;

import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.ProductVariant;

/**
 * A good, physical or digital, offered for sale in the catalog. Physical
 * products require inventory tracking and shipping; digital products are
 * delivered immediately after payment confirmation.
 *
 * <p>The status and the variants only change through {@link #changeStatus}
 * and {@link #replaceVariants}; the seller and the price are fixed when the
 * product is created.</p>
 */
public class Product {

    private final String identifier;
    private final String name;
    private final ProductType productType;
    private List<ProductVariant> variants;
    private ProductStatus status;
    private final Seller seller;
    private final Money price;

    /** A reference or a new product without seller, price or variants yet. */
    public Product(String identifier, String name, ProductType productType, ProductStatus status) {
        this(identifier, name, productType, status, null, null, null);
    }

    /** A complete product, e.g. one being published or loaded from persistence. */
    public Product(String identifier, String name, ProductType productType, ProductStatus status, Seller seller,
                   Money price, List<ProductVariant> variants) {
        this.identifier = identifier;
        this.name = name;
        this.productType = productType;
        this.status = status;
        this.seller = seller;
        this.price = price;
        this.variants = copyOf(variants);
    }

    private static List<ProductVariant> copyOf(List<ProductVariant> variants) {
        return variants != null ? new ArrayList<>(variants) : new ArrayList<>();
    }

    /** Unique identifier of the product. */
    public String getIdentifier() {
        return identifier;
    }

    /** Commercial name of the product. */
    public String getName() {
        return name;
    }

    /** Physical or Digital. */
    public ProductType getProductType() {
        return productType;
    }

    /** Variations such as color, size, or model (may be empty). */
    public List<ProductVariant> getVariants() {
        return variants;
    }

    /** Published, Suspended, or Discontinued. */
    public ProductStatus getStatus() {
        return status;
    }

    /** Seller who owns and publishes the product. */
    public Seller getSeller() {
        return seller;
    }

    /** Current selling price per unit, snapshotted into OrderLine when an order is confirmed. */
    public Money getPrice() {
        return price;
    }

    /** Moves the product to {@code newStatus} (e.g. withdraws it from sale). */
    public void changeStatus(ProductStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Product " + identifier + " status must not be null");
        }
        status = newStatus;
    }

    /** Replaces every variant of the product; {@code null} leaves it without variants. */
    public void replaceVariants(List<ProductVariant> newVariants) {
        variants = copyOf(newVariants);
    }
}
