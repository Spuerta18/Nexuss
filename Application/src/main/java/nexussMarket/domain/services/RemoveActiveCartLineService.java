package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;

/** Removes a product from the authenticated buyer's active cart, through {@link RemoveCartLineService}. */
@Service
public class RemoveActiveCartLineService {

    private final ConsultActiveCartService consultActiveCartService;
    private final RemoveCartLineService removeCartLineService;

    public RemoveActiveCartLineService(ConsultActiveCartService consultActiveCartService,
                                       RemoveCartLineService removeCartLineService) {
        this.consultActiveCartService = consultActiveCartService;
        this.removeCartLineService = removeCartLineService;
    }

    public ShoppingCart execute(User actor, Product product) {
        ShoppingCart cart = consultActiveCartService.execute(actor);
        return removeCartLineService.execute(actor, cart, product);
    }
}
