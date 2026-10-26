package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ShipmentStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/** Confirms that a dispatched shipment reached the buyer, and moves its order to {@code DELIVERED}. */
@Service
public class ConfirmDeliveryService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public ConfirmDeliveryService(ShipmentRepositoryPort shipmentRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                  ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                  RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Shipment execute(User actor, Shipment shipmentReference) {
        String shipmentId = shipmentReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR);
        Shipment shipment = shipmentRepositoryPort.findById(shipmentId)
                .orElseThrow(() -> new EntityNotFoundException("No shipment found with id " + shipmentId));
        Order order = shipment.getOrder();
        ShipmentStatus previousStatus = shipment.getStatus();
        shipment.confirmDelivery();
        order.advanceTo(OrderStatus.DELIVERED);
        orderRepositoryPort.save(order);
        Shipment saved = shipmentRepositoryPort.save(shipment);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.SHIPMENT_DELIVERY, actor, AffectedEntityType.SHIPMENT, shipmentId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", shipment.getStatus().getCode(), "orderId", order.getIdentifier(),
                        "newOrderStatus", order.getStatus().getCode()));
        return saved;
    }
}
