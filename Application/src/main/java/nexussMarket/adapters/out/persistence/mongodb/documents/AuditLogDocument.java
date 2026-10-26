package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An {@code AuditLog}, stored in the append-only {@code audit_logs}
 * collection. {@code details} is an embedded subdocument. Entries written
 * before the domain audit model (with {@code userId} / {@code operation}
 * fields) have no {@code operationType} and are not read back.
 */
@Document(collection = "audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "performer_operation_date", def = "{'performedById': 1, 'operationDate': -1}"),
        @CompoundIndex(name = "affected_entity", def = "{'affectedEntityType': 1, 'affectedEntityId': 1}")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLogDocument {

    @Id
    private String id;

    @Indexed
    private String operationType;

    @Indexed
    private LocalDateTime operationDate;

    private String performedById;
    private String userRole;
    private String affectedEntityType;
    private String affectedEntityId;
    private Map<String, Object> details;
}
