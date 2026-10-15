package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Dispatches a packed shipment: the stock reserved for each order line leaves
 * its warehouse, and the order moves to {@code SHIPPED}.
 */
@Service
public class DispatchShipmentService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ConfirmInventoryOutboundService confirmInventoryOutboundService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public DispatchShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                                   OrderRepositoryPort orderRepositoryPort,
                                   ConfirmInventoryOutboundService confirmInventoryOutboundService,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.confirmInventoryOutboundService = confirmInventoryOutboundService;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public Shipment execute(User actor, Shipment shipmentReference) {
        String shipmentId = shipmentReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        Shipment shipment = shipmentRepositoryPort.findById(shipmentId)
                .orElseThrow(() -> new EntityNotFoundException("No shipment found with id " + shipmentId));
        Order order = shipment.getOrder();
        // Both transitions are validated before any stock leaves a warehouse.
        shipment.dispatch();
        order.advanceTo(OrderStatus.SHIPPED);
        for (OrderLine line : order.getLines()) {
            confirmInventoryOutboundService.execute(line.getProduct(), line.getWarehouse(), line.getQuantity());
        }
        orderRepositoryPort.save(order);
        return shipmentRepositoryPort.save(shipment);
    }
}
