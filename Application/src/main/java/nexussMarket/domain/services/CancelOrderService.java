package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidOrderStatusTransitionException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.ports.in.CancelOrderUseCase;
import nexussMarket.domain.ports.in.ReleaseInventoryReservationUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;

public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ReleaseInventoryReservationService releaseInventoryReservationService;

    public CancelOrderService(OrderRepositoryPort orderRepositoryPort,
                               ReleaseInventoryReservationService releaseInventoryReservationService) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.releaseInventoryReservationService = releaseInventoryReservationService;
    }

    @Override
    public Order execute(Command command) {
        Order order = orderRepositoryPort.findById(command.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + command.orderId()));

        if (order.getStatus() != OrderStatus.CART && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStatusTransitionException(
                    "Order " + command.orderId() + " cannot be cancelled from status " + order.getStatus().getCode());
        }

        for (OrderLine line : order.getLines()) {
            releaseInventoryReservationService.execute(new ReleaseInventoryReservationUseCase.Command(
                    line.getProduct().getIdentifier(), line.getWarehouseId(), line.getQuantity()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepositoryPort.save(order);
    }
}
