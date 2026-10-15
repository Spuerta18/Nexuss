package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;

@Service
public class CreateShoppingCartService {

    private final CartRepositoryPort cartRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public CreateShoppingCartService(CartRepositoryPort cartRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    public ShoppingCart execute(User actor, ShoppingCart cart) {
        User user = userRepositoryPort.findById(actor.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException("No buyer found with id " + actor.getIdentifier()));
        if (!(user instanceof Buyer buyer)) {
            throw new EntityNotFoundException("No buyer found with id " + actor.getIdentifier());
        }
        return cartRepositoryPort.save(new ShoppingCart(cart.getIdentifier(), buyer));
    }
}
