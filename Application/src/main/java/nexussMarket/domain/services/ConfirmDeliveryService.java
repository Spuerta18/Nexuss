package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConfirmDeliveryUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/** Confirms that a dispatched shipment reached the buyer, and moves its order to {@code DELIVERED}. */
public class ConfirmDeliveryService implements ConfirmDeliveryUseCase {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConfirmDeliveryService(ShipmentRepositoryPort shipmentRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                  ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public Shipment execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        Shipment shipment = shipmentRepositoryPort.findById(command.shipmentId())
                .orElseThrow(() -> new EntityNotFoundException("No shipment found with id " + command.shipmentId()));
        Order order = shipment.getOrder();
        shipment.confirmDelivery();
        order.advanceTo(OrderStatus.DELIVERED);
        orderRepositoryPort.save(order);
        return shipmentRepositoryPort.save(shipment);
    }
}
