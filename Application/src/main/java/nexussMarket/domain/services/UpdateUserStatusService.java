package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.UpdateUserStatusUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;

public class UpdateUserStatusService implements UpdateUserStatusUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public UpdateUserStatusService(UserRepositoryPort userRepositoryPort,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    @Override
    public User execute(User actor, Command command) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        User user = userRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + command.userId()));
        user.setStatus(command.status());
        return userRepositoryPort.save(user);
    }
}
