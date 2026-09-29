package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.CancelOrderUseCase;
import nexussMarket.domain.ports.in.ReleaseInventoryReservationUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Cancels an order and releases the stock reserved for its lines. Only an
 * administrator or the buyer who placed the order may cancel it; supervisors
 * can consult orders but never cancel them.
 */
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public CancelOrderService(OrderRepositoryPort orderRepositoryPort,
                               ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService,
                               AuthorizeOrderAccessService authorizeOrderAccessService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.releaseInventoryReservationUseCase = releaseInventoryReservationUseCase;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
    }

    @Override
    public Order execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER, SystemRole.ADMINISTRATOR);
        Order order = orderRepositoryPort.findById(command.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + command.orderId()));
        // With the role restricted above, this only lets administrators and the owning buyer through.
        authorizeOrderAccessService.execute(actor, order);
        order.cancel();
        for (OrderLine line : order.getLines()) {
            releaseInventoryReservationUseCase.execute(new ReleaseInventoryReservationUseCase.Command(
                    line.getProduct().getIdentifier(), line.getWarehouse().getIdentifier(), line.getQuantity()));
        }
        return orderRepositoryPort.save(order);
    }
}
