package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.ShoppingCart;

public interface RemoveCartLineUseCase {

    record Command(String cartId, String productId) {}

    ShoppingCart execute(Command command);
}
