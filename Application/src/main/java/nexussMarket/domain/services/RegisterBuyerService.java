package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.ports.in.RegisterBuyerUseCase;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterBuyerService implements RegisterBuyerUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public RegisterBuyerService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public Buyer execute(Command command) {
        if (userRepositoryPort.existsById(command.identifier())) {
            throw new DuplicateResourceException("A user with identifier " + command.identifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        Buyer buyer = new Buyer(command.identifier(), command.fullName(), command.email(),
                UserStatus.ACTIVE, CustomerStatus.ENABLED);
        buyer.setPrimaryAddress(command.primaryAddress());
        buyer.setPasswordHash(passwordHasherPort.hash(command.password()));
        return (Buyer) userRepositoryPort.save(buyer);
    }
}
