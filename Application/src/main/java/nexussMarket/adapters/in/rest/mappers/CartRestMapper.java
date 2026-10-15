package nexussMarket.adapters.in.rest.mappers;

import nexussMarket.adapters.in.rest.requests.CreateCartRequest;
import nexussMarket.adapters.in.rest.responses.CartLineResponse;
import nexussMarket.adapters.in.rest.responses.CartResponse;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;

/** Converts cart requests into domain models and carts into {@link CartResponse}. */
public final class CartRestMapper {

    private CartRestMapper() {
    }

    /** A new, empty cart: the service sets the authenticated buyer as its owner. */
    public static ShoppingCart newCart(CreateCartRequest request) {
        return new ShoppingCart(request.cartId(), null);
    }

    public static CartResponse toResponse(ShoppingCart cart) {
        return new CartResponse(cart.getIdentifier(),
                cart.getBuyer() != null ? cart.getBuyer().getIdentifier() : null,
                cart.getLines().stream().map(CartRestMapper::toResponse).toList());
    }

    private static CartLineResponse toResponse(CartLine line) {
        Product product = line.getProduct();
        return new CartLineResponse(product.getIdentifier(), product.getName(), line.getQuantity(),
                CatalogRestMapper.toResponse(product.getPrice()));
    }
}
