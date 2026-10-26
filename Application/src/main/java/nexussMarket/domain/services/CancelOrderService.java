package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Cancels an order and releases the stock reserved for its lines. Only an
 * administrator or the buyer who placed the order may cancel it; supervisors
 * can consult orders but never cancel them.
 */
@Service
public class CancelOrderService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ReleaseInventoryReservationService releaseInventoryReservationService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public CancelOrderService(OrderRepositoryPort orderRepositoryPort,
                               ReleaseInventoryReservationService releaseInventoryReservationService,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService,
                               AuthorizeOrderAccessService authorizeOrderAccessService,
                               RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.releaseInventoryReservationService = releaseInventoryReservationService;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Order execute(User actor, Order orderReference) {
        String orderId = orderReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER, SystemRole.ADMINISTRATOR);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
        // With the role restricted above, this only lets administrators and the owning buyer through.
        authorizeOrderAccessService.execute(actor, order);
        OrderStatus previousStatus = order.getStatus();
        order.cancel();
        for (OrderLine line : order.getLines()) {
            releaseInventoryReservationService.execute(line.getProduct(), line.getWarehouse(), line.getQuantity());
        }
        Order saved = orderRepositoryPort.save(order);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.ORDER_CANCELLATION, actor, AffectedEntityType.ORDER, orderId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", order.getStatus().getCode()));
        return saved;
    }
}
