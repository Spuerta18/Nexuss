package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class RegisterSupervisorService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public RegisterSupervisorService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Supervisor execute(User actor, Supervisor supervisor, String password) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        if (userRepositoryPort.existsById(supervisor.getIdentifier())) {
            throw new DuplicateResourceException("A user with identifier " + supervisor.getIdentifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(supervisor.getEmail())) {
            throw new DuplicateResourceException("A user with email " + supervisor.getEmail() + " already exists");
        }
        Supervisor newSupervisor = new Supervisor(supervisor.getIdentifier(), supervisor.getFullName(),
                supervisor.getEmail(), UserStatus.ACTIVE);
        newSupervisor.setPasswordHash(passwordHasherPort.hash(password));
        Supervisor saved = (Supervisor) userRepositoryPort.save(newSupervisor);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.SUPERVISOR_REGISTRATION, actor, AffectedEntityType.USER, saved.getIdentifier()),
                AuditDetails.of("newStatus", saved.getStatus().getCode()));
        return saved;
    }
}
