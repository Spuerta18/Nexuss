package nexussMarket.domain.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.ProductVariant;

/**
 * A good, physical or digital, offered for sale in the catalog. Physical
 * products require inventory tracking and shipping; digital products are
 * delivered immediately after payment confirmation.
 */
public class Product {

    private String identifier;
    private String name;
    private ProductType productType;
    private List<ProductVariant> variants = new ArrayList<>();
    private ProductStatus status;
    private Seller seller;
    private BigDecimal price;

    public Product(String identifier, String name, ProductType productType, ProductStatus status) {
        this.identifier = identifier;
        this.name = name;
        this.productType = productType;
        this.status = status;
    }

    /** Unique identifier of the product. */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /** Commercial name of the product. */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Physical or Digital. */
    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    /** Variations such as color, size, or model (may be empty). */
    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants != null ? variants : new ArrayList<>();
    }

    /** Published, Suspended, or Discontinued. */
    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    /** Seller who owns and publishes the product. */
    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    /** Current selling price per unit, snapshotted into OrderLine when an order is confirmed. */
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}