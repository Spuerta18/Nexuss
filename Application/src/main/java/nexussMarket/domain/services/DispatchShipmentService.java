package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConfirmInventoryOutboundUseCase;
import nexussMarket.domain.ports.in.DispatchShipmentUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Dispatches a packed shipment: the stock reserved for each order line leaves
 * its warehouse, and the order moves to {@code SHIPPED}.
 */
public class DispatchShipmentService implements DispatchShipmentUseCase {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ConfirmInventoryOutboundUseCase confirmInventoryOutboundUseCase;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public DispatchShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                                   OrderRepositoryPort orderRepositoryPort,
                                   ConfirmInventoryOutboundUseCase confirmInventoryOutboundUseCase,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.confirmInventoryOutboundUseCase = confirmInventoryOutboundUseCase;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public Shipment execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        Shipment shipment = shipmentRepositoryPort.findById(command.shipmentId())
                .orElseThrow(() -> new EntityNotFoundException("No shipment found with id " + command.shipmentId()));
        Order order = shipment.getOrder();
        // Both transitions are validated before any stock leaves a warehouse.
        shipment.dispatch();
        order.advanceTo(OrderStatus.SHIPPED);
        for (OrderLine line : order.getLines()) {
            confirmInventoryOutboundUseCase.execute(new ConfirmInventoryOutboundUseCase.Command(
                    line.getProduct().getIdentifier(), line.getWarehouse().getIdentifier(), line.getQuantity()));
        }
        orderRepositoryPort.save(order);
        return shipmentRepositoryPort.save(shipment);
    }
}
