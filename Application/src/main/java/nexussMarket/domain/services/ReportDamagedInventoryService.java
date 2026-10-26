package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.InventoryMovement;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeWarehouseOperationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.InventoryMovementType;
import nexussMarket.domain.valueobjects.OperationType;

@Service
public class ReportDamagedInventoryService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public ReportDamagedInventoryService(InventoryRepositoryPort inventoryRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            AuthorizeWarehouseOperationService authorizeWarehouseOperationService,
            RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.inventoryMovementRepositoryPort = inventoryMovementRepositoryPort;
        this.authorizeWarehouseOperationService = authorizeWarehouseOperationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public InventoryItem execute(User actor, Product product, Warehouse warehouse, int quantity) {
        String productId = product.getIdentifier();
        String warehouseId = warehouse.getIdentifier();
        InventoryItem item = inventoryRepositoryPort
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No inventory item found for product " + productId + " in warehouse " + warehouseId));
        authorizeWarehouseOperationService.execute(actor, item.getWarehouse());
        int previousDamaged = item.getDamagedQuantity();
        item.markDamaged(quantity);
        InventoryItem saved = inventoryRepositoryPort.save(item);
        // Damaged units leave the sellable stock, so they are recorded as a negative ADJUSTMENT.
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.ADJUSTMENT, -quantity));
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.INVENTORY_DAMAGE_REPORT, actor, AffectedEntityType.INVENTORY_ITEM, item.getIdentifier()),
                AuditDetails.of("productId", productId, "warehouseId", warehouseId,
                        "quantity", quantity, "previousDamagedQuantity", previousDamaged,
                        "newDamagedQuantity", item.getDamagedQuantity()));
        return saved;
    }
}
