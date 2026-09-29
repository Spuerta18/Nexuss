package nexussMarket.domain.services.authorization;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;

/**
 * Allows an active {@link Administrator} or {@link LogisticsOperator} on any
 * warehouse, or the {@link Seller} who owns a Seller-owned warehouse.
 */
public class AuthorizeWarehouseOperationService {

    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public AuthorizeWarehouseOperationService(ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    public void execute(User actor, Warehouse warehouse) {
        validateUserAuthorizationStatusService.execute(actor);
        if (actor instanceof Administrator || actor instanceof LogisticsOperator) {
            return;
        }
        if (actor instanceof Seller && warehouse.getOwner() != null
                && warehouse.getOwner().getIdentifier().equals(actor.getIdentifier())) {
            return;
        }
        throw new OperationNotAllowedException(
                "User " + actor.getIdentifier() + " is not allowed to operate warehouse " + warehouse.getIdentifier());
    }
}
