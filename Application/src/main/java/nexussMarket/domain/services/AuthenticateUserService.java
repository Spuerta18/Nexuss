package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.AuthenticateUserUseCase;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.UserStatus;

public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public AuthenticateUserService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public User execute(Command command) {
        // Same message for unknown email and wrong password, so accounts cannot be enumerated.
        User user = userRepositoryPort.findByEmail(command.email())
                .filter(u -> u.getPasswordHash() != null
                        && passwordHasherPort.matches(command.password(), u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new OperationNotAllowedException("User " + user.getIdentifier() + " is not active");
        }
        return user;
    }
}
