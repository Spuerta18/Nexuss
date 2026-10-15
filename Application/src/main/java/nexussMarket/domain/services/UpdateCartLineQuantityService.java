package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

@Service
public class UpdateCartLineQuantityService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public UpdateCartLineQuantityService(CartRepositoryPort cartRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            AuthorizeCartAccessService authorizeCartAccessService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
    }

    public ShoppingCart execute(User actor, ShoppingCart cartReference, Product productReference, int quantity) {
        String cartId = cartReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        String productId = productReference.getIdentifier();
        ShoppingCart cart = cartRepositoryPort.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + cartId));
        authorizeCartAccessService.execute(actor, cart);

        CartLine line = cart.getLines().stream()
                .filter(l -> l.getProduct().getIdentifier().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "No cart line found for product " + productId + " in cart " + cartId));

        try {
            line.setQuantity(quantity);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quantity for product " + productId
                    + " in cart " + cartId + ": " + e.getMessage(), e);
        }

        return cartRepositoryPort.save(cart);
    }
}
