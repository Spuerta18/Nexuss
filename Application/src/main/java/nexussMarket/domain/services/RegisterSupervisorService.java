package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.ports.in.RegisterSupervisorUseCase;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterSupervisorService implements RegisterSupervisorUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public RegisterSupervisorService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public Supervisor execute(Command command) {
        if (userRepositoryPort.existsById(command.identifier())) {
            throw new DuplicateResourceException("A user with identifier " + command.identifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        Supervisor supervisor = new Supervisor(command.identifier(), command.fullName(),
                command.email(), UserStatus.ACTIVE);
        supervisor.setPasswordHash(passwordHasherPort.hash(command.password()));
        return (Supervisor) userRepositoryPort.save(supervisor);
    }
}
