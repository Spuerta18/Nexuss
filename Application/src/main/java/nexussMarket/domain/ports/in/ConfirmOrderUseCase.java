package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Order;

public interface ConfirmOrderUseCase {

    record Command(String cartId, String orderId) {}

    Order execute(Command command);
}
