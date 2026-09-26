package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.ports.in.RegisterSupervisorUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterSupervisorService implements RegisterSupervisorUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public RegisterSupervisorService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public Supervisor execute(Command command) {
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        Supervisor supervisor = new Supervisor(command.identifier(), command.fullName(),
                command.email(), UserStatus.ACTIVE);
        return (Supervisor) userRepositoryPort.save(supervisor);
    }
}
