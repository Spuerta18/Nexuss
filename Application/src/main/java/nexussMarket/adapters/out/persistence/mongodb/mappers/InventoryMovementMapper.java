package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryMovementDocument;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.InventoryMovementType;

/**
 * Converts {@link InventoryMovement} to and from
 * {@link InventoryMovementDocument}. The document {@code id} is left empty so
 * MongoDB generates it; the product and warehouse must be resolved by the
 * caller.
 */
public final class InventoryMovementMapper {

    private InventoryMovementMapper() {
    }

    public static InventoryMovementDocument toDocument(InventoryMovement movement) {
        InventoryMovementDocument document = new InventoryMovementDocument();
        document.setProductId(movement.getProduct().getIdentifier());
        document.setWarehouseId(movement.getWarehouse().getIdentifier());
        document.setType(movement.getType().getCode());
        document.setQuantity(movement.getQuantity());
        document.setOccurredAt(movement.getOccurredAt());
        return document;
    }

    /** {@code product} and {@code warehouse} are the entities referenced by the document. */
    public static InventoryMovement toDomain(InventoryMovementDocument document, Product product, Warehouse warehouse) {
        return new InventoryMovement(product, warehouse,
                CatalogCodes.resolve(InventoryMovementType::fromCode, document.getType(), "InventoryMovementType"),
                document.getQuantity(), document.getOccurredAt());
    }
}
