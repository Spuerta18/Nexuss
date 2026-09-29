package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.models.Order;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(String identifier);

    List<Order> findAllByBuyerId(String buyerId);
}
