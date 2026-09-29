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
 * A {@code Refund}. The return request is referenced by ID; a return request
 * has exactly one refund, so {@code returnRequestId} is unique.
 * {@code processedAt} is absent until the refund is processed.
 */
@Document(collection = "refunds")
@Getter
@Setter
@NoArgsConstructor
public class RefundDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String returnRequestId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
