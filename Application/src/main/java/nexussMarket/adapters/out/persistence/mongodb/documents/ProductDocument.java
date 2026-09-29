package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A catalog {@code Product}. The seller is referenced by ID; variants are
 * value objects and are embedded.
 */
@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductDocument {

    @Id
    private String id;

    private String name;
    private String productType;

    @Indexed
    private String status;

    @Indexed
    private String sellerId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private List<ProductVariantDocument> variants = new ArrayList<>();

    /** Embedded {@code ProductVariant}. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductVariantDocument {

        private String attribute;
        private String value;
    }
}
