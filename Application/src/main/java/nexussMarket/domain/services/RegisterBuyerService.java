package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.ports.in.RegisterBuyerUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterBuyerService implements RegisterBuyerUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public RegisterBuyerService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public Buyer execute(Command command) {
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        Buyer buyer = new Buyer(command.identifier(), command.fullName(), command.email(),
                UserStatus.ACTIVE, CustomerStatus.ENABLED);
        buyer.setPrimaryAddress(command.primaryAddress());
        return (Buyer) userRepositoryPort.save(buyer);
    }
}
