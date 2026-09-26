package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.ports.in.RegisterLogisticsOperatorUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterLogisticsOperatorService implements RegisterLogisticsOperatorUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public RegisterLogisticsOperatorService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public LogisticsOperator execute(Command command) {
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        LogisticsOperator operator = new LogisticsOperator(command.identifier(), command.fullName(),
                command.email(), UserStatus.ACTIVE);
        return (LogisticsOperator) userRepositoryPort.save(operator);
    }
}
