package nexussMarket.domain.services;

import java.util.List;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ListBuyerOrdersUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/**
 * Lists the orders placed by a buyer, most recent first. Administrators and
 * supervisors may list any buyer's orders; a buyer only its own.
 */
public class ListBuyerOrdersService implements ListBuyerOrdersUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public ListBuyerOrdersService(OrderRepositoryPort orderRepositoryPort,
                                  ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    @Override
    public List<Order> execute(User actor, Query query) {
        authorize(actor, query.buyerId());
        return orderRepositoryPort.findAllByBuyerId(query.buyerId());
    }

    private void authorize(User actor, String buyerId) {
        validateUserAuthorizationStatusService.execute(actor);
        if (actor instanceof Administrator || actor instanceof Supervisor) {
            return;
        }
        if (actor instanceof Buyer && actor.getIdentifier().equals(buyerId)) {
            return;
        }
        throw new OperationNotAllowedException(
                "User " + actor.getIdentifier() + " is not allowed to list the orders of buyer " + buyerId);
    }
}
