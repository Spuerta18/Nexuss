package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;

/**
 * Changes the quantity of a product in the authenticated buyer's active cart,
 * through {@link UpdateCartLineQuantityService}.
 */
@Service
public class UpdateActiveCartLineQuantityService {

    private final ConsultActiveCartService consultActiveCartService;
    private final UpdateCartLineQuantityService updateCartLineQuantityService;

    public UpdateActiveCartLineQuantityService(ConsultActiveCartService consultActiveCartService,
                                               UpdateCartLineQuantityService updateCartLineQuantityService) {
        this.consultActiveCartService = consultActiveCartService;
        this.updateCartLineQuantityService = updateCartLineQuantityService;
    }

    public ShoppingCart execute(User actor, Product product, int quantity) {
        ShoppingCart cart = consultActiveCartService.execute(actor);
        return updateCartLineQuantityService.execute(actor, cart, product, quantity);
    }
}
