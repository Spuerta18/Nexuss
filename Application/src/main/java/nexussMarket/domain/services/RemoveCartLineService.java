package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

@Service
public class RemoveCartLineService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public RemoveCartLineService(CartRepositoryPort cartRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            AuthorizeCartAccessService authorizeCartAccessService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
    }

    public ShoppingCart execute(User actor, ShoppingCart cartReference, Product productReference) {
        String cartId = cartReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        String productId = productReference.getIdentifier();
        ShoppingCart cart = cartRepositoryPort.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + cartId));
        authorizeCartAccessService.execute(actor, cart);

        boolean removed = cart.getLines().removeIf(line -> line.getProduct().getIdentifier().equals(productId));
        if (!removed) {
            throw new EntityNotFoundException(
                    "No cart line found for product " + productId + " in cart " + cartId);
        }

        return cartRepositoryPort.save(cart);
    }
}
