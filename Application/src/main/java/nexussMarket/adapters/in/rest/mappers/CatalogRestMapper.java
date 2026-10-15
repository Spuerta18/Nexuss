package nexussMarket.adapters.in.rest.mappers;

import java.util.List;

import nexussMarket.adapters.in.rest.exception.InvalidRequestException;
import nexussMarket.adapters.in.rest.requests.MoneyRequest;
import nexussMarket.adapters.in.rest.requests.PublishProductRequest;
import nexussMarket.adapters.in.rest.requests.VariantRequest;
import nexussMarket.adapters.in.rest.responses.MoneyResponse;
import nexussMarket.adapters.in.rest.responses.ProductResponse;
import nexussMarket.adapters.in.rest.responses.VariantResponse;
import nexussMarket.domain.enums.VariantAttributeType;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.ProductVariant;

/**
 * Converts catalog requests into domain models and catalog models
 * ({@link Product}, {@link Money}, variants) into responses.
 */
public final class CatalogRestMapper {

    private CatalogRestMapper() {
    }

    /**
     * A new product. Its status and seller are placeholders: the service
     * publishes it with the authenticated seller as owner.
     */
    public static Product newProduct(PublishProductRequest request) {
        return new Product(request.identifier(), request.name(),
                RestCodes.resolve(ProductType::fromCode, request.productType(), "product type"), null, null,
                toMoney(request.price()), toVariants(request.variants()));
    }

    public static ProductStatus productStatus(String code) {
        return RestCodes.resolve(ProductStatus::fromCode, code, "product status");
    }

    public static Money toMoney(MoneyRequest request) {
        return Money.of(request.amount(), RestCodes.resolve(Currency::fromCode, request.currency(), "currency"));
    }

    /** {@code null} maps to an empty list. */
    public static List<ProductVariant> toVariants(List<VariantRequest> variants) {
        if (variants == null) {
            return List.of();
        }
        return variants.stream().map(v -> new ProductVariant(variantAttribute(v.attribute()), v.value())).toList();
    }

    private static VariantAttributeType variantAttribute(String name) {
        try {
            return VariantAttributeType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown variant attribute '" + name + "'");
        }
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getIdentifier(), product.getName(),
                RestCodes.code(product.getProductType()), RestCodes.code(product.getStatus()),
                product.getSeller() != null ? product.getSeller().getIdentifier() : null,
                toResponse(product.getPrice()), toVariantResponses(product.getVariants()));
    }

    public static List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(CatalogRestMapper::toResponse).toList();
    }

    /** {@code null} stays {@code null}. */
    public static MoneyResponse toResponse(Money money) {
        return money != null ? new MoneyResponse(money.getAmount(), money.getCurrency().getCode()) : null;
    }

    private static List<VariantResponse> toVariantResponses(List<ProductVariant> variants) {
        return variants.stream().map(v -> new VariantResponse(v.attribute().name(), v.value())).toList();
    }
}
