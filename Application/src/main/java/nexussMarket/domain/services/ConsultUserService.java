package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConsultUserUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Returns any user's data. Only administrators and supervisors may consult it. */
public class ConsultUserService implements ConsultUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultUserService(UserRepositoryPort userRepositoryPort,
                              ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public User execute(User actor, Query query) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        return userRepositoryPort.findById(query.userId())
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + query.userId()));
    }
}
