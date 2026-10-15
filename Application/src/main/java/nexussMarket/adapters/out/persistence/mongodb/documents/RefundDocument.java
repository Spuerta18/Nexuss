package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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

    private MoneyDocument amount;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
