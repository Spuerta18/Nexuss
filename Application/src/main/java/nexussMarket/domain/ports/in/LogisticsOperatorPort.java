package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.OrderStatus;

/** Operations a {@link LogisticsOperator} can execute on orders, shipments and warehouse stock. */
public interface LogisticsOperatorPort {

    Order consultOrder(User user, Order order);

    Order advanceOrderStatus(User user, Order order, OrderStatus targetStatus);

    /** {@code shipment} carries the identifier of the shipment to create and the order it delivers. */
    Shipment createShipment(User user, Shipment shipment);

    Shipment packShipment(User user, Shipment shipment);

    Shipment dispatchShipment(User user, Shipment shipment);

    Shipment confirmDelivery(User user, Shipment shipment);

    InventoryItem reportDamagedInventory(User user, Product product, Warehouse warehouse, int quantity);

    InventoryItem registerInventoryReturn(User user, Product product, Warehouse warehouse, int quantity);

    List<InventoryItem> consultInventory(User user, Product product);
}
