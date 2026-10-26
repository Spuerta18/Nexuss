package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.TokenServicePort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class AuthenticateUserService {

    static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenServicePort tokenServicePort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public AuthenticateUserService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
                                   TokenServicePort tokenServicePort,
                                   RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenServicePort = tokenServicePort;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public AuthenticationResult execute(String email, String password) {
        // The same error for an unknown email, a wrong password and a user who is not ACTIVE, so a failed login
        // never reveals whether an account exists or whether the password was right.
        User user = userRepositoryPort.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null
                        && passwordHasherPort.matches(password, u.getPasswordHash()))
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        AuthenticationResult result = new AuthenticationResult(user, tokenServicePort.generateToken(user));
        // Only successful logins are recorded; the token itself never is.
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.USER_LOGIN, user, AffectedEntityType.USER, user.getIdentifier()),
                AuditDetails.of());
        return result;
    }
}
