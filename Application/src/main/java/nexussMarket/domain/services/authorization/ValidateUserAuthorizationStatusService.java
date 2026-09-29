package nexussMarket.domain.services.authorization;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Base authorization check, used by every other authorization service: the
 * actor must be present and {@code ACTIVE}.
 */
public class ValidateUserAuthorizationStatusService {

    public void execute(User actor) {
        if (actor == null) {
            throw new OperationNotAllowedException("An authenticated user is required to perform this operation");
        }
        if (actor.getStatus() != UserStatus.ACTIVE) {
            throw new OperationNotAllowedException("User " + actor.getIdentifier() + " is not active");
        }
    }
}
