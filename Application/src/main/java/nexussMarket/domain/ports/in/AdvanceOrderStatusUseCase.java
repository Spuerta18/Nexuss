package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.valueobjects.OrderStatus;

public interface AdvanceOrderStatusUseCase {

    record Command(String orderId, OrderStatus targetStatus) {}

    Order execute(Command command);
}
