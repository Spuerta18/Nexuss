package nexussMarket.adapters.out.persistence.mongodb.documents;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An {@code Operation}, stored in the append-only {@code operations}
 * collection. Catalog values are stored by code and the performer by
 * identifier.
 */
@Document(collection = "operations")
@CompoundIndexes({
        @CompoundIndex(name = "performer_execution", def = "{'performedById': 1, 'executionDate': -1}"),
        @CompoundIndex(name = "affected_entity", def = "{'affectedEntityType': 1, 'affectedEntityId': 1}")
})
@Getter
@Setter
@NoArgsConstructor
public class OperationDocument {

    @Id
    private String id;

    @Indexed
    private String operationType;

    @Indexed
    private LocalDateTime executionDate;

    private String performedById;
    private String affectedEntityType;
    private String affectedEntityId;
}
