package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;

public interface ConsultCartUseCase {

    record Query(String cartId) {}

    ShoppingCart execute(User actor, Query query);
}
