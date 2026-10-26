package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/**
 * Logs a user out by revoking every access token issued to them: the stored
 * user's {@code authTokenVersion} is raised, so the token used for this call
 * and any other one stop being accepted. Idempotent: logging out again simply
 * revokes again. Available to every role.
 */
@Service
public class LogoutService {

    private final UserRepositoryPort userRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public LogoutService(UserRepositoryPort userRepositoryPort,
                         RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public void execute(User actor) {
        // The stored user, not the reference received: its version is the one tokens are checked against.
        User user = userRepositoryPort.findById(actor.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + actor.getIdentifier()));
        user.bumpAuthTokenVersion();
        userRepositoryPort.save(user);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.USER_LOGOUT, user, AffectedEntityType.USER, user.getIdentifier()),
                AuditDetails.of());
    }
}
