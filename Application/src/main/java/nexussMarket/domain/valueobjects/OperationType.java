package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Kind of state-changing business operation recorded as an
 * {@link nexussMarket.domain.models.Operation} and its
 * {@link nexussMarket.domain.models.AuditLog}. There is one code per operation
 * that changes the state of the platform; reads are never recorded.
 */
public final class OperationType extends DomainCatalog {

    // Users and sessions
    public static final OperationType BUYER_REGISTRATION =
            new OperationType("BUYER_REGISTRATION", "Buyer Registration", "A buyer registered themselves.");
    public static final OperationType SELLER_REGISTRATION =
            new OperationType("SELLER_REGISTRATION", "Seller Registration", "An administrator registered a seller.");
    public static final OperationType SUPERVISOR_REGISTRATION = new OperationType("SUPERVISOR_REGISTRATION",
            "Supervisor Registration", "An administrator registered a supervisor.");
    public static final OperationType LOGISTICS_OPERATOR_REGISTRATION = new OperationType(
            "LOGISTICS_OPERATOR_REGISTRATION", "Logistics Operator Registration",
            "An administrator registered a logistics operator.");
    public static final OperationType USER_LOGIN =
            new OperationType("USER_LOGIN", "User Login", "A user logged in successfully and received a token.");
    public static final OperationType USER_LOGOUT =
            new OperationType("USER_LOGOUT", "User Logout", "A user logged out; every token issued before is revoked.");
    public static final OperationType PASSWORD_CHANGE = new OperationType("PASSWORD_CHANGE", "Password Change",
            "A user changed their password; every token issued before is revoked.");
    public static final OperationType USER_STATUS_CHANGE = new OperationType("USER_STATUS_CHANGE",
            "User Status Change", "An administrator changed the operational status of a user.");
    public static final OperationType SELLER_STATUS_CHANGE = new OperationType("SELLER_STATUS_CHANGE",
            "Seller Status Change", "An administrator changed the commercial status of a seller.");

    // Catalog
    public static final OperationType PRODUCT_PUBLICATION =
            new OperationType("PRODUCT_PUBLICATION", "Product Publication", "A seller published a product.");
    public static final OperationType PRODUCT_STATUS_CHANGE = new OperationType("PRODUCT_STATUS_CHANGE",
            "Product Status Change", "The publication status of a product changed.");
    public static final OperationType PRODUCT_VARIANTS_UPDATE = new OperationType("PRODUCT_VARIANTS_UPDATE",
            "Product Variants Update", "The variants of a product were replaced.");

    // Warehouses and inventory
    public static final OperationType WAREHOUSE_REGISTRATION =
            new OperationType("WAREHOUSE_REGISTRATION", "Warehouse Registration", "A warehouse was registered.");
    public static final OperationType WAREHOUSE_DEACTIVATION =
            new OperationType("WAREHOUSE_DEACTIVATION", "Warehouse Deactivation", "A warehouse was deactivated.");
    public static final OperationType INVENTORY_INBOUND =
            new OperationType("INVENTORY_INBOUND", "Inventory Inbound", "Stock was received in a warehouse.");
    public static final OperationType INVENTORY_ADJUSTMENT = new OperationType("INVENTORY_ADJUSTMENT",
            "Inventory Adjustment", "The available stock of a warehouse was corrected.");
    public static final OperationType INVENTORY_DAMAGE_REPORT = new OperationType("INVENTORY_DAMAGE_REPORT",
            "Inventory Damage Report", "Units were reported as damaged and left the sellable stock.");
    public static final OperationType INVENTORY_RETURN = new OperationType("INVENTORY_RETURN", "Inventory Return",
            "Returned units went back to the sellable stock of a warehouse.");

    // Carts and orders
    public static final OperationType CART_CREATION =
            new OperationType("CART_CREATION", "Cart Creation", "A buyer created a shopping cart.");
    public static final OperationType CART_LINE_ADDITION =
            new OperationType("CART_LINE_ADDITION", "Cart Line Addition", "A product was added to a cart.");
    public static final OperationType CART_LINE_QUANTITY_UPDATE = new OperationType("CART_LINE_QUANTITY_UPDATE",
            "Cart Line Quantity Update", "The quantity of a cart line changed.");
    public static final OperationType CART_LINE_REMOVAL =
            new OperationType("CART_LINE_REMOVAL", "Cart Line Removal", "A product was removed from a cart.");
    public static final OperationType ORDER_CONFIRMATION = new OperationType("ORDER_CONFIRMATION",
            "Order Confirmation", "A cart was turned into an order and its stock reserved.");
    public static final OperationType ORDER_CANCELLATION = new OperationType("ORDER_CANCELLATION",
            "Order Cancellation", "An order was cancelled and its reserved stock released.");
    public static final OperationType ORDER_STATUS_ADVANCE = new OperationType("ORDER_STATUS_ADVANCE",
            "Order Status Advance", "An order moved to its next status outside the shipment lifecycle.");

    // Shipments
    public static final OperationType SHIPMENT_CREATION =
            new OperationType("SHIPMENT_CREATION", "Shipment Creation", "The shipment of a paid order was created.");
    public static final OperationType SHIPMENT_PACKING =
            new OperationType("SHIPMENT_PACKING", "Shipment Packing", "The goods of a shipment were packed.");
    public static final OperationType SHIPMENT_DISPATCH = new OperationType("SHIPMENT_DISPATCH", "Shipment Dispatch",
            "A shipment left the warehouse and its order moved to SHIPPED.");
    public static final OperationType SHIPMENT_DELIVERY = new OperationType("SHIPMENT_DELIVERY", "Shipment Delivery",
            "A shipment reached the buyer and its order moved to DELIVERED.");

    // Returns and refunds
    public static final OperationType RETURN_REQUEST =
            new OperationType("RETURN_REQUEST", "Return Request", "A buyer requested the return of an order.");
    public static final OperationType RETURN_APPROVAL = new OperationType("RETURN_APPROVAL", "Return Approval",
            "An administrator approved a return request and a refund was created.");
    public static final OperationType RETURN_REJECTION =
            new OperationType("RETURN_REJECTION", "Return Rejection", "An administrator rejected a return request.");
    public static final OperationType REFUND_PROCESSING =
            new OperationType("REFUND_PROCESSING", "Refund Processing", "A pending refund was paid to the buyer.");

    private OperationType(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code OperationType} value list. */
    public static List<OperationType> values() {
        return List.of(BUYER_REGISTRATION, SELLER_REGISTRATION, SUPERVISOR_REGISTRATION,
                LOGISTICS_OPERATOR_REGISTRATION, USER_LOGIN, USER_LOGOUT, PASSWORD_CHANGE, USER_STATUS_CHANGE,
                SELLER_STATUS_CHANGE, PRODUCT_PUBLICATION, PRODUCT_STATUS_CHANGE, PRODUCT_VARIANTS_UPDATE,
                WAREHOUSE_REGISTRATION, WAREHOUSE_DEACTIVATION, INVENTORY_INBOUND, INVENTORY_ADJUSTMENT,
                INVENTORY_DAMAGE_REPORT, INVENTORY_RETURN, CART_CREATION, CART_LINE_ADDITION,
                CART_LINE_QUANTITY_UPDATE, CART_LINE_REMOVAL, ORDER_CONFIRMATION, ORDER_CANCELLATION,
                ORDER_STATUS_ADVANCE, SHIPMENT_CREATION, SHIPMENT_PACKING, SHIPMENT_DISPATCH, SHIPMENT_DELIVERY,
                RETURN_REQUEST, RETURN_APPROVAL, RETURN_REJECTION, REFUND_PROCESSING);
    }

    /** Resolves an {@code OperationType} from its code, or {@code null} if unknown. */
    public static OperationType fromCode(String code) {
        return fromCode(values(), code);
    }
}
