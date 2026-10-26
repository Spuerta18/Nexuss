package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class UpdateUserStatusService {

    private final UserRepositoryPort userRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public UpdateUserStatusService(UserRepositoryPort userRepositoryPort,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                   RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public User execute(User actor, User target, UserStatus status) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        User user = userRepositoryPort.findById(target.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + target.getIdentifier()));
        UserStatus previousStatus = user.getStatus();
        user.setStatus(status);
        User saved = userRepositoryPort.save(user);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.USER_STATUS_CHANGE, actor, AffectedEntityType.USER, user.getIdentifier()),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", user.getStatus().getCode(), "role", user.getRole().getCode()));
        return saved;
    }
}
