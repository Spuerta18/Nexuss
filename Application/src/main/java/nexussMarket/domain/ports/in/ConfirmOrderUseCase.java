package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.User;

public interface ConfirmOrderUseCase {

    record Command(String cartId, String orderId) {}

    Order execute(User actor, Command command);
}
