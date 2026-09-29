package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.ports.in.AdvanceOrderStatusUseCase;
import nexussMarket.domain.ports.in.ConfirmInventoryOutboundUseCase;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;

/**
 * Moves an order one step forward in its lifecycle. When the order is
 * {@code SHIPPED}, the stock reserved for each line leaves the warehouse.
 */
public class AdvanceOrderStatusService implements AdvanceOrderStatusUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ConfirmInventoryOutboundUseCase confirmInventoryOutboundUseCase;

    public AdvanceOrderStatusService(OrderRepositoryPort orderRepositoryPort,
                                     ConfirmInventoryOutboundUseCase confirmInventoryOutboundUseCase) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.confirmInventoryOutboundUseCase = confirmInventoryOutboundUseCase;
    }

    @Override
    public Order execute(Command command) {
        Order order = orderRepositoryPort.findById(command.orderId())
                .orElseThrow(() -> new EntityNotFoundException("No order found with id " + command.orderId()));
        order.advanceTo(command.targetStatus());
        if (order.getStatus() == OrderStatus.SHIPPED) {
            for (OrderLine line : order.getLines()) {
                confirmInventoryOutboundUseCase.execute(new ConfirmInventoryOutboundUseCase.Command(
                        line.getProduct().getIdentifier(), line.getWarehouse().getIdentifier(), line.getQuantity()));
            }
        }
        return orderRepositoryPort.save(order);
    }
}
