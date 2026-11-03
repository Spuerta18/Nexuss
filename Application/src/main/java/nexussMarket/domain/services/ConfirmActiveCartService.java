package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;

/**
 * Turns the authenticated buyer's active cart into an order, through
 * {@link ConfirmOrderService}: stock reservation with rollback, frozen prices,
 * buyer checks and audit are unchanged. {@code newOrder} carries the order
 * identifier, or none to have the server generate it.
 */
@Service
public class ConfirmActiveCartService {

    private final ConsultActiveCartService consultActiveCartService;
    private final ConfirmOrderService confirmOrderService;

    public ConfirmActiveCartService(ConsultActiveCartService consultActiveCartService,
                                    ConfirmOrderService confirmOrderService) {
        this.consultActiveCartService = consultActiveCartService;
        this.confirmOrderService = confirmOrderService;
    }

    public Order execute(User actor, Order newOrder) {
        ShoppingCart cart = consultActiveCartService.execute(actor);
        return confirmOrderService.execute(actor, cart, newOrder);
    }
}
