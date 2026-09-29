package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nexussMarket.adapters.out.persistence.mongodb.documents.ProductDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ProductDocument.ProductVariantDocument;
import nexussMarket.domain.enums.VariantAttributeType;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.ProductVariant;

/**
 * Converts {@link Product} to and from {@link ProductDocument}. The seller is
 * stored as {@code sellerId} and must be resolved by the caller; variants are
 * embedded.
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductDocument toDocument(Product product) {
        ProductDocument document = new ProductDocument();
        document.setId(product.getIdentifier());
        document.setName(product.getName());
        document.setProductType(product.getProductType().getCode());
        document.setStatus(product.getStatus().getCode());
        document.setSellerId(product.getSeller() != null ? product.getSeller().getIdentifier() : null);
        document.setPrice(product.getPrice());
        document.setVariants(product.getVariants().stream().map(ProductMapper::toDocument).toList());
        return document;
    }

    /** {@code seller} is the seller referenced by {@code sellerId}. */
    public static Product toDomain(ProductDocument document, Seller seller) {
        Product product = new Product(document.getId(), document.getName(),
                CatalogCodes.resolve(ProductType::fromCode, document.getProductType(), "ProductType"),
                CatalogCodes.resolve(ProductStatus::fromCode, document.getStatus(), "ProductStatus"));
        product.setSeller(seller);
        product.setPrice(document.getPrice());
        product.setVariants(toDomain(document.getVariants()));
        return product;
    }

    private static ProductVariantDocument toDocument(ProductVariant variant) {
        ProductVariantDocument document = new ProductVariantDocument();
        document.setAttribute(variant.attribute().name());
        document.setValue(variant.value());
        return document;
    }

    private static List<ProductVariant> toDomain(List<ProductVariantDocument> variants) {
        if (variants == null) {
            return null;
        }
        return variants.stream()
                .map(v -> new ProductVariant(VariantAttributeType.valueOf(v.getAttribute()), v.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
