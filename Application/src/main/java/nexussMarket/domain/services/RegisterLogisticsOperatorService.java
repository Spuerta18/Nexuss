package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class RegisterLogisticsOperatorService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RegisterLogisticsOperatorService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public LogisticsOperator execute(User actor, LogisticsOperator logisticsOperator, String password) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        if (userRepositoryPort.existsById(logisticsOperator.getIdentifier())) {
            throw new DuplicateResourceException("A user with identifier " + logisticsOperator.getIdentifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(logisticsOperator.getEmail())) {
            throw new DuplicateResourceException("A user with email " + logisticsOperator.getEmail() + " already exists");
        }
        LogisticsOperator newLogisticsOperator = new LogisticsOperator(logisticsOperator.getIdentifier(), logisticsOperator.getFullName(),
                logisticsOperator.getEmail(), UserStatus.ACTIVE);
        newLogisticsOperator.setPasswordHash(passwordHasherPort.hash(password));
        return (LogisticsOperator) userRepositoryPort.save(newLogisticsOperator);
    }
}
