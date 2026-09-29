package nexussMarket.domain.services.authorization;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;

/**
 * Allows an active {@link Administrator} or {@link Supervisor}, or the
 * {@link Buyer} who placed the order.
 */
public class AuthorizeOrderAccessService {

    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public AuthorizeOrderAccessService(ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    public void execute(User actor, Order order) {
        validateUserAuthorizationStatusService.execute(actor);
        if (actor instanceof Administrator || actor instanceof Supervisor) {
            return;
        }
        if (actor instanceof Buyer && order.getBuyer() != null
                && order.getBuyer().getIdentifier().equals(actor.getIdentifier())) {
            return;
        }
        throw new OperationNotAllowedException(
                "User " + actor.getIdentifier() + " is not allowed to access order " + order.getIdentifier());
    }
}
