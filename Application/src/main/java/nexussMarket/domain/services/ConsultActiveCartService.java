package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * The active cart of the authenticated buyer: a buyer has at most one cart,
 * and it stops being active when it becomes an order. Fails with
 * {@link EntityNotFoundException} when the buyer has none. The other
 * active-cart services locate the cart through this one.
 */
@Service
public class ConsultActiveCartService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultActiveCartService(CartRepositoryPort cartRepositoryPort,
                                    ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public ShoppingCart execute(User actor) {
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        return cartRepositoryPort.findByBuyerId(actor.getIdentifier()).stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Buyer " + actor.getIdentifier() + " has no active cart"));
    }
}
