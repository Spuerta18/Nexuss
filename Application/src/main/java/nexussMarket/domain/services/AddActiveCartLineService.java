package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Adds a product to the authenticated buyer's active cart, creating the cart
 * (with a generated identifier) when the buyer has none. Creation and addition
 * go through {@link CreateShoppingCartService} and {@link AddCartLineService},
 * so their rules and audit records apply unchanged. If another request creates
 * the active cart first, the line is added to that cart.
 */
@Service
public class AddActiveCartLineService {

    private final CartRepositoryPort cartRepositoryPort;
    private final CreateShoppingCartService createShoppingCartService;
    private final AddCartLineService addCartLineService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public AddActiveCartLineService(CartRepositoryPort cartRepositoryPort,
                                    CreateShoppingCartService createShoppingCartService,
                                    AddCartLineService addCartLineService,
                                    ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.createShoppingCartService = createShoppingCartService;
        this.addCartLineService = addCartLineService;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public ShoppingCart execute(User actor, Product product, int quantity) {
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        ShoppingCart cart = cartRepositoryPort.findByBuyerId(actor.getIdentifier()).stream().findFirst()
                .orElseGet(() -> createActiveCart(actor));
        return addCartLineService.execute(actor, cart, product, quantity);
    }

    private ShoppingCart createActiveCart(User actor) {
        try {
            return createShoppingCartService.execute(actor, new ShoppingCart(null, null));
        } catch (DuplicateResourceException e) {
            // A concurrent request created the active cart between the lookup and the insert.
            return cartRepositoryPort.findByBuyerId(actor.getIdentifier()).stream().findFirst().orElseThrow(() -> e);
        }
    }
}
