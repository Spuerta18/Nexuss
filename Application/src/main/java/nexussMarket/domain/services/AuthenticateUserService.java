package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.AuthenticateUserUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;

public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public AuthenticateUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User execute(Command command) {
        return userRepositoryPort.findByEmail(command.email())
                .orElseThrow(() -> new EntityNotFoundException("No user found with email " + command.email()));
    }
}
