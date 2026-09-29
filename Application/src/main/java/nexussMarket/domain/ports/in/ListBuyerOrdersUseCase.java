package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;

public interface ListBuyerOrdersUseCase {

    record Query(String buyerId) {}

    List<Order> execute(User actor, Query query);
}
