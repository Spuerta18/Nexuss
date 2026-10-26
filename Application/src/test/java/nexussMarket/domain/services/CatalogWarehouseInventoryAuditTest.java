package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

import nexussMarket.domain.enums.VariantAttributeType;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.ProductVariant;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/** Catalog, warehouse and inventory changes record their operation and audit log. */
class CatalogWarehouseInventoryAuditTest extends AuditedServiceTestSupport {

    private final ProductRepositoryPort productRepositoryPort = mock(ProductRepositoryPort.class);
    private final UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
    private final WarehouseRepositoryPort warehouseRepositoryPort = mock(WarehouseRepositoryPort.class);
    private final InventoryRepositoryPort inventoryRepositoryPort = mock(InventoryRepositoryPort.class);
    private final InventoryMovementRepositoryPort movementRepositoryPort = mock(InventoryMovementRepositoryPort.class);

    private final Seller seller = seller();
    private final Product product = product(seller);
    private final Warehouse warehouse = warehouse(seller);

    @BeforeEach
    void repositoriesReturnWhatTheyStore() {
        when(productRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(warehouseRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(inventoryRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(seller));
        when(productRepositoryPort.findById("P-1")).thenReturn(Optional.of(product));
        when(warehouseRepositoryPort.findById("W-1")).thenReturn(Optional.of(warehouse));
    }

    @Test
    void productPublicationRecordsStatusAndPrice() {
        Product draft = new Product("P-2", "Mouse", ProductType.PHYSICAL, null, null, cop(500), List.of());

        new PublishProductService(productRepositoryPort, userRepositoryPort, audit).execute(seller, draft);

        AuditLog entry = assertAudited(OperationType.PRODUCT_PUBLICATION, seller, AffectedEntityType.PRODUCT, "P-2");
        assertEquals("PUBLISHED", entry.getDetails().get("newStatus"));
        assertEquals("500", entry.getDetails().get("price"));
        assertEquals("COP", entry.getDetails().get("currency"));
    }

    @Test
    void productStatusChangeRecordsPreviousAndNewStatus() {
        new UpdateProductStatusService(productRepositoryPort, authorizeProduct, audit)
                .execute(seller, product, ProductStatus.SUSPENDED);

        AuditLog entry = assertAudited(OperationType.PRODUCT_STATUS_CHANGE, seller, AffectedEntityType.PRODUCT, "P-1");
        assertEquals("PUBLISHED", entry.getDetails().get("previousStatus"));
        assertEquals("SUSPENDED", entry.getDetails().get("newStatus"));
    }

    @Test
    void productVariantsUpdateIsRecorded() {
        new UpdateProductVariantsService(productRepositoryPort, authorizeProduct, audit)
                .execute(seller, product, List.of(new ProductVariant(VariantAttributeType.COLOR, "Black")));

        AuditLog entry = assertAudited(OperationType.PRODUCT_VARIANTS_UPDATE, seller, AffectedEntityType.PRODUCT,
                "P-1");
        assertEquals(0, entry.getDetails().get("previousVariantCount"));
        assertEquals(1, entry.getDetails().get("newVariantCount"));
    }

    @Test
    void sellerWarehouseRegistrationIsRecorded() {
        Warehouse request = new Warehouse("W-2", "Second", "3 Dock Street", WarehouseOwnerType.SELLER);

        new RegisterWarehouseService(warehouseRepositoryPort, userRepositoryPort, validateRole, audit)
                .execute(seller, request);

        AuditLog entry = assertAudited(OperationType.WAREHOUSE_REGISTRATION, seller, AffectedEntityType.WAREHOUSE,
                "W-2");
        assertEquals("SELLER", entry.getDetails().get("ownerType"));
        assertEquals("S-1", entry.getDetails().get("ownerId"));
    }

    @Test
    void marketplaceWarehouseRegistrationIsRecordedWithoutOwner() {
        Administrator admin = administrator();
        Warehouse request = new Warehouse("W-3", "Hub", "4 Dock Street", WarehouseOwnerType.MARKETPLACE);

        new RegisterWarehouseService(warehouseRepositoryPort, userRepositoryPort, validateRole, audit)
                .execute(admin, request);

        AuditLog entry = assertAudited(OperationType.WAREHOUSE_REGISTRATION, admin, AffectedEntityType.WAREHOUSE,
                "W-3");
        assertEquals("MARKETPLACE", entry.getDetails().get("ownerType"));
        assertEquals(false, entry.getDetails().containsKey("ownerId"));
    }

    @Test
    void warehouseDeactivationIsRecorded() {
        Administrator admin = administrator();

        new DeactivateWarehouseService(warehouseRepositoryPort, authorizeWarehouse, audit).execute(admin, warehouse);

        AuditLog entry = assertAudited(OperationType.WAREHOUSE_DEACTIVATION, admin, AffectedEntityType.WAREHOUSE,
                "W-1");
        assertEquals(true, entry.getDetails().get("previousActive"));
        assertEquals(false, entry.getDetails().get("newActive"));
    }

    @Test
    void inventoryInboundRecordsQuantities() {
        stock(4, 0);

        new RegisterInventoryInboundService(inventoryRepositoryPort, movementRepositoryPort, productRepositoryPort,
                warehouseRepositoryPort, authorizeWarehouse, audit).execute(seller, product, warehouse, 6);

        AuditLog entry = assertAudited(OperationType.INVENTORY_INBOUND, seller, AffectedEntityType.INVENTORY_ITEM,
                "P-1:W-1");
        assertEquals(6, entry.getDetails().get("quantity"));
        assertEquals(4, entry.getDetails().get("previousAvailableQuantity"));
        assertEquals(10, entry.getDetails().get("newAvailableQuantity"));
    }

    @Test
    void inventoryAdjustmentRecordsQuantities() {
        stock(10, 0);

        new AdjustInventoryService(inventoryRepositoryPort, movementRepositoryPort, authorizeWarehouse, audit)
                .execute(seller, product, warehouse, -3);

        AuditLog entry = assertAudited(OperationType.INVENTORY_ADJUSTMENT, seller, AffectedEntityType.INVENTORY_ITEM,
                "P-1:W-1");
        assertEquals(-3, entry.getDetails().get("quantityDelta"));
        assertEquals(7, entry.getDetails().get("newAvailableQuantity"));
    }

    @Test
    void damagedInventoryReportRecordsQuantities() {
        LogisticsOperator operator = logisticsOperator();
        stock(10, 1);

        new ReportDamagedInventoryService(inventoryRepositoryPort, movementRepositoryPort, authorizeWarehouse, audit)
                .execute(operator, product, warehouse, 2);

        AuditLog entry = assertAudited(OperationType.INVENTORY_DAMAGE_REPORT, operator,
                AffectedEntityType.INVENTORY_ITEM, "P-1:W-1");
        assertEquals(1, entry.getDetails().get("previousDamagedQuantity"));
        assertEquals(3, entry.getDetails().get("newDamagedQuantity"));
    }

    @Test
    void inventoryReturnRecordsQuantities() {
        LogisticsOperator operator = logisticsOperator();
        stock(5, 0);

        new RegisterInventoryReturnService(inventoryRepositoryPort, movementRepositoryPort, authorizeWarehouse, audit)
                .execute(operator, product, warehouse, 2);

        AuditLog entry = assertAudited(OperationType.INVENTORY_RETURN, operator, AffectedEntityType.INVENTORY_ITEM,
                "P-1:W-1");
        assertEquals(7, entry.getDetails().get("newAvailableQuantity"));
    }

    private void stock(int available, int damaged) {
        when(inventoryRepositoryPort.findByProductIdAndWarehouseId("P-1", "W-1"))
                .thenReturn(Optional.of(new InventoryItem(product, warehouse, available, 0, damaged)));
    }
}
