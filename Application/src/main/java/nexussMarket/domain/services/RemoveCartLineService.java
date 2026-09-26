package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.ports.in.RemoveCartLineUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;

public class RemoveCartLineService implements RemoveCartLineUseCase {

    private final CartRepositoryPort cartRepositoryPort;

    public RemoveCartLineService(CartRepositoryPort cartRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
    }

    @Override
    public ShoppingCart execute(Command command) {
        ShoppingCart cart = cartRepositoryPort.findById(command.cartId())
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + command.cartId()));

        boolean removed = cart.getLines().removeIf(line -> line.getProduct().getIdentifier().equals(command.productId()));
        if (!removed) {
            throw new EntityNotFoundException(
                    "No cart line found for product " + command.productId() + " in cart " + command.cartId());
        }

        return cartRepositoryPort.save(cart);
    }
}
