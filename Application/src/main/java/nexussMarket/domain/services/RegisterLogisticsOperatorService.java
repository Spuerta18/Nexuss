package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.ports.in.RegisterLogisticsOperatorUseCase;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterLogisticsOperatorService implements RegisterLogisticsOperatorUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public RegisterLogisticsOperatorService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public LogisticsOperator execute(Command command) {
        if (userRepositoryPort.existsById(command.identifier())) {
            throw new DuplicateResourceException("A user with identifier " + command.identifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        LogisticsOperator operator = new LogisticsOperator(command.identifier(), command.fullName(),
                command.email(), UserStatus.ACTIVE);
        operator.setPasswordHash(passwordHasherPort.hash(command.password()));
        return (LogisticsOperator) userRepositoryPort.save(operator);
    }
}
