package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConsultCartUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;

/** Returns a shopping cart to its buyer, an administrator or a supervisor. */
public class ConsultCartService implements ConsultCartUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public ConsultCartService(CartRepositoryPort cartRepositoryPort,
                              AuthorizeCartAccessService authorizeCartAccessService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.authorizeCartAccessService = authorizeCartAccessService;
    }

    @Override
    public ShoppingCart execute(User actor, Query query) {
        ShoppingCart cart = cartRepositoryPort.findById(query.cartId())
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + query.cartId()));
        authorizeCartAccessService.execute(actor, cart);
        return cart;
    }
}
