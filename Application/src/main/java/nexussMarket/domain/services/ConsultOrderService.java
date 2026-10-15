package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeOrderAccessService;

/**
 * Returns an order to its buyer, an administrator or a supervisor. Unlike
 * {@link CancelOrderService}, supervisors are allowed: this is a read.
 */
@Service
public class ConsultOrderService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final AuthorizeOrderAccessService authorizeOrderAccessService;

    public ConsultOrderService(OrderRepositoryPort orderRepositoryPort,
                               AuthorizeOrderAccessService authorizeOrderAccessService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.authorizeOrderAccessService = authorizeOrderAccessService;
    }

    public Order execute(User actor, Order orderReference) {
        String orderId = orderReference.getIdentifier();
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
        authorizeOrderAccessService.execute(actor, order);
        return order;
    }
}
