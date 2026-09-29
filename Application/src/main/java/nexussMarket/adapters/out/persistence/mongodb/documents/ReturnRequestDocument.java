package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A {@code ReturnRequest}. The order, the buyer and the deciding user are
 * referenced by ID; an order has at most one return request, so
 * {@code orderId} is unique. {@code decidedAt} and {@code decidedById} are
 * absent until the request is decided.
 */
@Document(collection = "return_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequestDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderId;

    @Indexed
    private String buyerId;

    private String reason;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private String decidedById;
}
