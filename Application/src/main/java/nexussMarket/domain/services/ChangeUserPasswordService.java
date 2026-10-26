package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.IncorrectCurrentPasswordException;
import nexussMarket.domain.exceptions.InvalidPasswordException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/**
 * Changes the authenticated user's own password. The stored user must be
 * {@code ACTIVE}, the current password must match, and the new one must have
 * at least {@value #MIN_PASSWORD_LENGTH} characters. The new password is
 * hashed (BCrypt, through {@link PasswordHasherPort}) and every access token
 * issued before is revoked, so the user has to log in again. Available to
 * every role. Neither password is ever audited.
 */
@Service
public class ChangeUserPasswordService {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public ChangeUserPasswordService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
                                     ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService,
                                     RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public void execute(User actor, String currentPassword, String newPassword) {
        User user = userRepositoryPort.findById(actor.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + actor.getIdentifier()));
        validateUserAuthorizationStatusService.execute(user);
        if (currentPassword == null || user.getPasswordHash() == null
                || !passwordHasherPort.matches(currentPassword, user.getPasswordHash())) {
            throw new IncorrectCurrentPasswordException("The current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidPasswordException(
                    "The new password must have at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        user.setPasswordHash(passwordHasherPort.hash(newPassword));
        user.bumpAuthTokenVersion();
        userRepositoryPort.save(user);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.PASSWORD_CHANGE, user, AffectedEntityType.USER, user.getIdentifier()),
                AuditDetails.of());
    }
}
