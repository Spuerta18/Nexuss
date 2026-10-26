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
public class RegisterInventoryReturnService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final AuthorizeWarehouseOperationService authorizeWarehouseOperationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public RegisterInventoryReturnService(InventoryRepositoryPort inventoryRepositoryPort,
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
        int previousAvailable = item.getAvailableQuantity();
        item.receive(quantity);
        InventoryItem saved = inventoryRepositoryPort.save(item);
        inventoryMovementRepositoryPort.save(
                InventoryMovement.of(item, InventoryMovementType.RETURN, quantity));
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.INVENTORY_RETURN, actor, AffectedEntityType.INVENTORY_ITEM, item.getIdentifier()),
                AuditDetails.of("productId", productId, "warehouseId", warehouseId,
                        "quantity", quantity, "previousAvailableQuantity", previousAvailable,
                        "newAvailableQuantity", item.getAvailableQuantity()));
        return saved;
    }
}
