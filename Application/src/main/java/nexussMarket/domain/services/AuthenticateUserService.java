package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.TokenServicePort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class AuthenticateUserService {

    static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenServicePort tokenServicePort;

    public AuthenticateUserService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
                                   TokenServicePort tokenServicePort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenServicePort = tokenServicePort;
    }

    public AuthenticationResult execute(String email, String password) {
        // The same error for an unknown email, a wrong password and a user who is not ACTIVE, so a failed login
        // never reveals whether an account exists or whether the password was right.
        User user = userRepositoryPort.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null
                        && passwordHasherPort.matches(password, u.getPasswordHash()))
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        return new AuthenticationResult(user, tokenServicePort.generateToken(user));
    }
}
