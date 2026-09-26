package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.ports.in.UpdateCartLineQuantityUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;

public class UpdateCartLineQuantityService implements UpdateCartLineQuantityUseCase {

    private final CartRepositoryPort cartRepositoryPort;

    public UpdateCartLineQuantityService(CartRepositoryPort cartRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
    }

    @Override
    public ShoppingCart execute(Command command) {
        ShoppingCart cart = cartRepositoryPort.findById(command.cartId())
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + command.cartId()));

        CartLine line = cart.getLines().stream()
                .filter(l -> l.getProduct().getIdentifier().equals(command.productId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "No cart line found for product " + command.productId() + " in cart " + command.cartId()));

        try {
            line.setQuantity(command.quantity());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quantity for product " + command.productId()
                    + " in cart " + command.cartId() + ": " + e.getMessage(), e);
        }

        return cartRepositoryPort.save(cart);
    }
}
