package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConsultOrderUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;

/**
 * Returns an order to its buyer, an administrator or a supervisor. Unlike
 * {@link CancelOrderService}, supervisors are allowed: this is a read.
 */
public class ConsultOrderService implements ConsultOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public ConsultOrderService(OrderRepositoryPort orderRepositoryPort,
                               AuthorizeOrderAccessService authorizeOrderAccessService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
    }

    @Override
    public Order execute(User actor, Query query) {
        Order order = orderRepositoryPort.findById(query.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + query.orderId()));
        authorizeOrderAccessService.execute(actor, order);
        return order;
    }
}
