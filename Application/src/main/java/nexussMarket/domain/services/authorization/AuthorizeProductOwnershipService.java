package nexussMarket.domain.services.authorization;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;

/** Allows an active {@link Administrator}, or the {@link Seller} who owns the product. */
public class AuthorizeProductOwnershipService {

    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public AuthorizeProductOwnershipService(ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    public void execute(User actor, Product product) {
        validateUserAuthorizationStatusService.execute(actor);
        if (actor instanceof Administrator) {
            return;
        }
        if (actor instanceof Seller && product.getSeller() != null
                && product.getSeller().getIdentifier().equals(actor.getIdentifier())) {
            return;
        }
        throw new OperationNotAllowedException(
                "User " + actor.getIdentifier() + " is not allowed to manage product " + product.getIdentifier());
    }
}
