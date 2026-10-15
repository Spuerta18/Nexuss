package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/** Registers the shipment of a paid order, in {@code CREATED}. An order has at most one shipment. */
@Service
public class CreateShipmentService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public CreateShipmentService(ShipmentRepositoryPort shipmentRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public Shipment execute(User actor, Shipment shipment) {
        String shipmentId = shipment.getIdentifier();
        String orderId = shipment.getOrder().getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        if (shipmentRepositoryPort.findById(shipmentId).isPresent()) {
            throw new DuplicateResourceException("A shipment with id " + shipmentId + " already exists");
        }
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessRuleViolationException("Order " + order.getIdentifier() + " is "
                    + order.getStatus().getCode() + "; only PAID orders can be shipped");
        }
        if (shipmentRepositoryPort.findByOrderId(order.getIdentifier()).isPresent()) {
            throw new DuplicateResourceException("Order " + order.getIdentifier() + " already has a shipment");
        }
        return shipmentRepositoryPort.save(new Shipment(shipmentId, order));
    }
}
