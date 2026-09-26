package nexussMarket.domain.services;

import java.util.List;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.ports.in.AdvanceOrderStatusUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;

public class AdvanceOrderStatusService implements AdvanceOrderStatusUseCase {

    private static final List<OrderStatus> LIFECYCLE = List.of(
            OrderStatus.CART, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderRepositoryPort orderRepositoryPort;

    public AdvanceOrderStatusService(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    @Override
    public Order execute(Command command) {
        Order order = orderRepositoryPort.findById(command.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + command.orderId()));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + command.orderId() + " is DELIVERED and cannot be modified");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + command.orderId() + " is CANCELLED and cannot re-enter the order lifecycle");
        }
        if (command.targetStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + command.orderId() + " cannot be cancelled through AdvanceOrderStatusUseCase; use CancelOrderUseCase instead");
        }

        int currentIndex = LIFECYCLE.indexOf(order.getStatus());
        int targetIndex = LIFECYCLE.indexOf(command.targetStatus());
        if (targetIndex != currentIndex + 1) {
            throw new InvalidOrderStatusTransitionException("Cannot transition order " + command.orderId()
                    + " from " + order.getStatus().getCode() + " to " + command.targetStatus().getCode());
        }

        order.setStatus(command.targetStatus());
        return orderRepositoryPort.save(order);
    }
}
