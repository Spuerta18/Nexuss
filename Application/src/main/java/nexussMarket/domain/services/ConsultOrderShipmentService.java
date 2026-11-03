package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * The shipment of an order, for the buyer who placed it. Ownership is decided
 * by {@link AuthorizeOrderAccessService}. Fails with
 * {@link EntityNotFoundException} if the order does not exist or has not been
 * shipped yet.
 */
@Service
public class ConsultOrderShipmentService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public ConsultOrderShipmentService(OrderRepositoryPort orderRepositoryPort,
                                       ShipmentRepositoryPort shipmentRepositoryPort,
                                       ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                       AuthorizeOrderAccessService authorizeOrderAccessService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
    }

    public Shipment execute(User actor, Order orderReference) {
        String orderId = orderReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
        authorizeOrderAccessService.execute(actor, order);
        return shipmentRepositoryPort.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order " + orderId + " has no shipment yet"));
    }
}
