package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.ShoppingCart;

public interface AddCartLineUseCase {

    record Command(String cartId, String productId, int quantity) {}

    ShoppingCart execute(Command command);
}
