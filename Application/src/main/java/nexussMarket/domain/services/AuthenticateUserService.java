package nexussMarket.domain.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.TokenServicePort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.EmailAddress;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class AuthenticateUserService {

    static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenServicePort tokenServicePort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    /**
     * Hash of a random password nobody knows, made with the same hasher (and so the same cost) as the stored ones.
     * Created on the first login, not at startup.
     */
    private volatile String dummyHash;

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
        Optional<User> found = userRepositoryPort.findByEmail(EmailAddress.normalize(email));
        // BCrypt runs on every path, against a throwaway hash when the email is unknown, so the response time does
        // not reveal whether an account exists either.
        String passwordHash = found.map(User::getPasswordHash).orElse(null);
        boolean passwordMatches = passwordHasherPort.matches(password != null ? password : "",
                passwordHash != null ? passwordHash : dummyHash());
        User user = found
                .filter(u -> passwordHash != null && passwordMatches)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        AuthenticationResult result = new AuthenticationResult(user, tokenServicePort.generateToken(user));
        // Only successful logins are recorded; the token itself never is.
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.USER_LOGIN, user, AffectedEntityType.USER, user.getIdentifier()),
                AuditDetails.of());
        return result;
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            hash = passwordHasherPort.hash(UUID.randomUUID().toString());
            dummyHash = hash;
        }
        return hash;
    }
}
