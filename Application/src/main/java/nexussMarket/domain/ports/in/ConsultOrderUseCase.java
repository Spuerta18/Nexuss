package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;

public interface ConsultOrderUseCase {

    record Query(String orderId) {}

    Order execute(User actor, Query query);
}
