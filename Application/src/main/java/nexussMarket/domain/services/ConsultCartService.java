package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Returns a shopping cart to the buyer who owns it. */
@Service
public class ConsultCartService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public ConsultCartService(CartRepositoryPort cartRepositoryPort,
                              ValidateRoleAuthorizationService validateRoleAuthorizationService,
                              AuthorizeCartAccessService authorizeCartAccessService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
    }

    public ShoppingCart execute(User actor, ShoppingCart cartReference) {
        String cartId = cartReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        ShoppingCart cart = cartRepositoryPort.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + cartId));
        authorizeCartAccessService.execute(actor, cart);
        return cart;
    }
}
