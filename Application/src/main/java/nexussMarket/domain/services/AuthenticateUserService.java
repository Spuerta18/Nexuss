package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.TokenServicePort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class AuthenticateUserService {

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
        // Same message for unknown email and wrong password, so accounts cannot be enumerated.
        User user = userRepositoryPort.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null
                        && passwordHasherPort.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new OperationNotAllowedException("User " + user.getIdentifier() + " is not active");
        }
        return new AuthenticationResult(user, tokenServicePort.generateToken(user));
    }
}
