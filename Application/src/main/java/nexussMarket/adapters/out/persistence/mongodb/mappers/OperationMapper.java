package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.OperationDocument;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/** Converts {@link Operation} to and from {@link OperationDocument}. */
public final class OperationMapper {

    private OperationMapper() {
    }

    public static OperationDocument toDocument(Operation operation) {
        OperationDocument document = new OperationDocument();
        document.setId(operation.getIdentifier());
        document.setOperationType(operation.getOperationType().getCode());
        document.setExecutionDate(operation.getExecutionDate());
        document.setPerformedById(operation.getPerformedBy() != null ? operation.getPerformedBy().getIdentifier() : null);
        document.setAffectedEntityType(operation.getAffectedEntityType().getCode());
        document.setAffectedEntityId(operation.getAffectedEntityId());
        return document;
    }

    /** {@code performedBy} is the user stored as {@code performedById}, already resolved; may be {@code null}. */
    public static Operation toDomain(OperationDocument document, User performedBy) {
        return new Operation(document.getId(),
                CatalogCodes.resolve(OperationType::fromCode, document.getOperationType(), "OperationType"),
                document.getExecutionDate(), performedBy,
                CatalogCodes.resolve(AffectedEntityType::fromCode, document.getAffectedEntityType(),
                        "AffectedEntityType"),
                document.getAffectedEntityId());
    }
}
