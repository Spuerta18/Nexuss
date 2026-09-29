package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.CreateShoppingCartUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;

public class CreateShoppingCartService implements CreateShoppingCartUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public CreateShoppingCartService(CartRepositoryPort cartRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public ShoppingCart execute(Command command) {
        User user = userRepositoryPort.findById(command.buyerId())
                .orElseThrow(() -> new EntityNotFoundException("No buyer found with id " + command.buyerId()));
        if (!(user instanceof Buyer buyer)) {
            throw new EntityNotFoundException("No buyer found with id " + command.buyerId());
        }
        return cartRepositoryPort.save(new ShoppingCart(command.cartId(), buyer));
    }
}
