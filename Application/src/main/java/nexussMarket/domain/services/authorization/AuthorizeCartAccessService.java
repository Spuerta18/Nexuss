package nexussMarket.domain.services.authorization;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;

/**
 * Allows an active {@link Administrator} or {@link Supervisor}, or the
 * {@link Buyer} who owns the cart.
 */
public class AuthorizeCartAccessService {

    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public AuthorizeCartAccessService(ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    public void execute(User actor, ShoppingCart cart) {
        validateUserAuthorizationStatusService.execute(actor);
        if (actor instanceof Administrator || actor instanceof Supervisor) {
            return;
        }
        if (actor instanceof Buyer && cart.getBuyer() != null
                && cart.getBuyer().getIdentifier().equals(actor.getIdentifier())) {
            return;
        }
        throw new OperationNotAllowedException(
                "User " + actor.getIdentifier() + " is not allowed to access cart " + cart.getIdentifier());
    }
}
