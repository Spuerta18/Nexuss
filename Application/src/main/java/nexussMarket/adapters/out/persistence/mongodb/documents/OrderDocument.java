package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
 * An {@code Order}. Order lines are embedded; the buyer, and each line's
 * product and warehouse, are referenced by ID. Each line keeps the product
 * name and unit price as they were when the order was confirmed.
 */
@Document(collection = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderDocument {

    @Id
    private String id;

    @Indexed
    private String buyerId;

    private String status;
    private LocalDateTime createdAt;
    private List<OrderLineDocument> lines = new ArrayList<>();

    /** Embedded {@code OrderLine}. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderLineDocument {

        private String productId;
        private String productName;
        private int quantity;

        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal unitPrice;

        private String warehouseId;
    }
}
