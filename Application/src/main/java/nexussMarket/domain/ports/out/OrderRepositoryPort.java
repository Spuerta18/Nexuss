package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Order;

public interface OrderRepositoryPort {

    /**
     * Stores a new order. Fails with {@link DuplicateResourceException} if a order with the same
     * identifier already exists; the stored one is never replaced.
     */
    Order insert(Order order);

    /** Stores the changes of an existing order. */
    Order save(Order order);

    Optional<Order> findById(String identifier);

    List<Order> findAllByBuyerId(String buyerId);
}
