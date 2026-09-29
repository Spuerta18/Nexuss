package nexussMarket.domain.valueobjects;

import java.util.Objects;

import nexussMarket.domain.enums.VariantAttributeType;

/**
 * One variation of a {@link nexussMarket.domain.models.Product}, such as
 * color "red" or size "M". Immutable and compared by value.
 */
public record ProductVariant(VariantAttributeType attribute, String value) {

    public ProductVariant {
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }
}
