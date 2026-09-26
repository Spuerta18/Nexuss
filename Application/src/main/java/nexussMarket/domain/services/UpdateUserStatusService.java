package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.UpdateUserStatusUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;

public class UpdateUserStatusService implements UpdateUserStatusUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public UpdateUserStatusService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User execute(Command command) {
        User user = userRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new EntityNotFoundException("No user found with id " + command.userId()));
        user.setStatus(command.status());
        return userRepositoryPort.save(user);
    }
}
