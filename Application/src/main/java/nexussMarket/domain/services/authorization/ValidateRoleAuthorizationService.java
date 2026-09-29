package nexussMarket.domain.services.authorization;

import java.util.Arrays;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.SystemRole;

/** Allows an active actor whose role is one of {@code allowedRoles}. */
public class ValidateRoleAuthorizationService {

    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public ValidateRoleAuthorizationService(ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    public void execute(User actor, SystemRole... allowedRoles) {
        validateUserAuthorizationStatusService.execute(actor);
        if (Arrays.stream(allowedRoles).noneMatch(role -> role == actor.getRole())) {
            throw new OperationNotAllowedException("User " + actor.getIdentifier() + " with role "
                    + actor.getRole().getCode() + " is not allowed to perform this operation");
        }
    }
}
