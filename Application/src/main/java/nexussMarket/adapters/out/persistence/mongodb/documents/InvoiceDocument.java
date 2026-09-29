package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An {@code Invoice}. The order and the buyer are referenced by ID; an order
 * has exactly one invoice, so {@code orderId} is unique. Written once, never
 * updated.
 */
@Document(collection = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderId;

    @Indexed
    private String buyerId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalAmount;

    private LocalDateTime issuedAt;
}
