package nexussMarket.adapters.out.persistence.memory;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryItemDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.InventoryMovementDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.InvoiceDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.OperationDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ProductDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.RefundDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ShoppingCartDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;

/**
 * The store of the {@code memory} profile: one {@link InMemoryCollection} per
 * MongoDB collection, holding the same documents, with the same unique keys
 * (the {@code unique} indexes) and the same versioned collections. Data lives
 * only as long as the application runs.
 */
@Component
@Profile("memory")
public class InMemoryDatabase {

    final InMemoryCollection<UserDocument> users = InMemoryCollection.versioned("users", UserDocument::getId,
            List.of(UserDocument::getEmail), UserDocument::getVersion, UserDocument::setVersion);
    final InMemoryCollection<ProductDocument> products =
            InMemoryCollection.plain("products", ProductDocument::getId, List.of());
    final InMemoryCollection<WarehouseDocument> warehouses =
            InMemoryCollection.plain("warehouses", WarehouseDocument::getId, List.of());
    final InMemoryCollection<InventoryItemDocument> inventoryItems = InMemoryCollection.versioned("inventory_items",
            InventoryItemDocument::getId, List.of(), InventoryItemDocument::getVersion,
            InventoryItemDocument::setVersion);
    final InMemoryCollection<InventoryMovementDocument> inventoryMovements =
            InMemoryCollection.plain("inventory_movements", InventoryMovementDocument::getId, List.of());
    final InMemoryCollection<ShoppingCartDocument> carts = InMemoryCollection.versioned("shopping_carts",
            ShoppingCartDocument::getId, List.of(ShoppingCartDocument::getBuyerId), ShoppingCartDocument::getVersion,
            ShoppingCartDocument::setVersion);
    final InMemoryCollection<OrderDocument> orders = InMemoryCollection.versioned("orders", OrderDocument::getId,
            List.of(), OrderDocument::getVersion, OrderDocument::setVersion);
    final InMemoryCollection<InvoiceDocument> invoices =
            InMemoryCollection.plain("invoices", InvoiceDocument::getId, List.of(InvoiceDocument::getOrderId));
    final InMemoryCollection<ShipmentDocument> shipments = InMemoryCollection.versioned("shipments",
            ShipmentDocument::getId, List.of(ShipmentDocument::getOrderId), ShipmentDocument::getVersion,
            ShipmentDocument::setVersion);
    final InMemoryCollection<ReturnRequestDocument> returnRequests = InMemoryCollection.versioned("return_requests",
            ReturnRequestDocument::getId, List.of(ReturnRequestDocument::getOrderId),
            ReturnRequestDocument::getVersion, ReturnRequestDocument::setVersion);
    final InMemoryCollection<RefundDocument> refunds = InMemoryCollection.versioned("refunds", RefundDocument::getId,
            List.of(RefundDocument::getReturnRequestId), RefundDocument::getVersion, RefundDocument::setVersion);
    final InMemoryCollection<OperationDocument> operations =
            InMemoryCollection.plain("operations", OperationDocument::getId, List.of());
    final InMemoryCollection<AuditLogDocument> auditLogs =
            InMemoryCollection.plain("audit_logs", AuditLogDocument::getId, List.of());
}
