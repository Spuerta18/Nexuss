package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.ports.in.CancelOrderUseCase;
import nexussMarket.domain.ports.in.ReleaseInventoryReservationUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;

public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase;

    public CancelOrderService(OrderRepositoryPort orderRepositoryPort,
                               ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.releaseInventoryReservationUseCase = releaseInventoryReservationUseCase;
    }

    @Override
    public Order execute(Command command) {
        Order order = orderRepositoryPort.findById(command.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + command.orderId()));
        order.cancel();
        for (OrderLine line : order.getLines()) {
            releaseInventoryReservationUseCase.execute(new ReleaseInventoryReservationUseCase.Command(
                    line.getProduct().getIdentifier(), line.getWarehouse().getIdentifier(), line.getQuantity()));
        }
        return orderRepositoryPort.save(order);
    }
}
