package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An append-only record of an operation performed by an authenticated user.
 * Cross-cutting: it does not persist any Domain Model.
 */
@Document(collection = "audit_logs")
@CompoundIndex(name = "user_occurred", def = "{'userId': 1, 'occurredAt': 1}")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogDocument {

    @Id
    private String id;

    private String userId;
    private String operation;
    private LocalDateTime occurredAt;
}
