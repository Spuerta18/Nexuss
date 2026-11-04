package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
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

/**
 * Dispatches a packed shipment: the stock reserved for each order line leaves
 * its warehouse, and the order moves to {@code SHIPPED}.
 *
 * <p>Order of effects: versioned shipment save, order save (re-read and
 * retried on a conflict), stock outbound (retried on conflicts), audit.</p>
 */
@Service
public class DispatchShipmentService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ConfirmInventoryOutboundService confirmInventoryOutboundService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public DispatchShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                                   OrderRepositoryPort orderRepositoryPort,
                                   ConfirmInventoryOutboundService confirmInventoryOutboundService,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                   RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.shipmentRepositoryPort = shipmentRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.confirmInventoryOutboundService = confirmInventoryOutboundService;
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
        // Both transitions are validated before any stock leaves a warehouse.
        shipment.dispatch();
        order.advanceTo(OrderStatus.SHIPPED);
        // Saved with its version before any side effect: of two concurrent dispatches only one gets past here.
        Shipment saved = shipmentRepositoryPort.save(shipment);
        order = OrderFollowUp.save(orderRepositoryPort, order, OrderStatus.SHIPPED);
        for (OrderLine line : order.getLines()) {
            if (!line.holdsStock()) {
                continue;
            }
            confirmInventoryOutboundService.execute(line.getProduct(), line.getWarehouse(), line.getQuantity());
        }
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.SHIPMENT_DISPATCH, actor, AffectedEntityType.SHIPMENT, shipmentId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", shipment.getStatus().getCode(), "orderId", order.getIdentifier(),
                        "newOrderStatus", order.getStatus().getCode()));
        return saved;
    }
}
