package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.ShoppingCart;

public interface CreateShoppingCartUseCase {

    record Command(String cartId, String buyerId) {}

    ShoppingCart execute(Command command);
}
