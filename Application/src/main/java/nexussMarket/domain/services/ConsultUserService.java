package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

/** Returns any user's data. Only administrators and supervisors may consult it. */
@Service
public class ConsultUserService {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultUserService(UserRepositoryPort userRepositoryPort,
                              ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public User execute(User actor, User target) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        return userRepositoryPort.findById(target.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + target.getIdentifier()));
    }
}
