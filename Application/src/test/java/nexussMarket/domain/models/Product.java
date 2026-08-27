package nexussMarket.domain.models;

import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;

/**
 * A good, physical or digital, offered for sale in the catalog. Physical
 * products require inventory tracking and shipping; digital products are
 * delivered immediately after payment confirmation.
 */
public class Product {

    private String identifier;
    private String name;
    private ProductType productType;
    private List<String> variants = new ArrayList<>();
    private ProductStatus status;
    private Seller seller;

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
    public List<String> getVariants() {
        return variants;
    }

    public void setVariants(List<String> variants) {
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
}