package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;

/**
 * Saves the order that follows a shipment transition. The versioned shipment
 * save has already decided which call wins, so the order is not the guard
 * here: if someone else saved it meanwhile, it is read again and moved to the
 * same status, retried like any other conflict ({@link OptimisticRetry}).
 */
final class OrderFollowUp {

    private OrderFollowUp() {
    }

    /** Saves {@code order}, already moved to {@code status}; on a conflict re-reads it and advances it again. */
    static Order save(OrderRepositoryPort orderRepositoryPort, Order order, OrderStatus status) {
        try {
            return orderRepositoryPort.save(order);
        } catch (ConcurrentUpdateException conflict) {
            String orderId = order.getIdentifier();
            return OptimisticRetry.run(() -> {
                Order current = orderRepositoryPort.findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("No order found with id " + orderId));
                if (current.getStatus() != status) {
                    current.advanceTo(status);
                }
                return orderRepositoryPort.save(current);
            });
        }
    }
}
