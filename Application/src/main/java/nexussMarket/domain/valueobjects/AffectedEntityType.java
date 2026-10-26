package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Kind of entity changed by an {@link nexussMarket.domain.models.Operation}.
 * Together with the entity's identifier it tells which record an operation and
 * its audit log refer to.
 */
public final class AffectedEntityType extends DomainCatalog {

    public static final AffectedEntityType USER =
            new AffectedEntityType("USER", "User", "A user of any role.");
    public static final AffectedEntityType PRODUCT =
            new AffectedEntityType("PRODUCT", "Product", "A product of the catalog.");
    public static final AffectedEntityType WAREHOUSE =
            new AffectedEntityType("WAREHOUSE", "Warehouse", "A Marketplace-owned or Seller-owned warehouse.");
    public static final AffectedEntityType INVENTORY_ITEM = new AffectedEntityType("INVENTORY_ITEM",
            "Inventory Item", "The stock of one product in one warehouse; identified as productId:warehouseId.");
    public static final AffectedEntityType SHOPPING_CART =
            new AffectedEntityType("SHOPPING_CART", "Shopping Cart", "A buyer's shopping cart.");
    public static final AffectedEntityType ORDER =
            new AffectedEntityType("ORDER", "Order", "An order placed by a buyer.");
    public static final AffectedEntityType SHIPMENT =
            new AffectedEntityType("SHIPMENT", "Shipment", "The shipment of an order.");
    public static final AffectedEntityType RETURN_REQUEST =
            new AffectedEntityType("RETURN_REQUEST", "Return Request", "A buyer's request to return an order.");
    public static final AffectedEntityType REFUND =
            new AffectedEntityType("REFUND", "Refund", "The refund of an approved return.");

    private AffectedEntityType(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code AffectedEntityType} value list. */
    public static List<AffectedEntityType> values() {
        return List.of(USER, PRODUCT, WAREHOUSE, INVENTORY_ITEM, SHOPPING_CART, ORDER, SHIPMENT, RETURN_REQUEST,
                REFUND);
    }

    /** Resolves an {@code AffectedEntityType} from its code, or {@code null} if unknown. */
    public static AffectedEntityType fromCode(String code) {
        return fromCode(values(), code);
    }
}
