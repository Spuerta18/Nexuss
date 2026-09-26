package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Order;

public interface CancelOrderUseCase {

    record Command(String orderId) {}

    Order execute(Command command);
}
