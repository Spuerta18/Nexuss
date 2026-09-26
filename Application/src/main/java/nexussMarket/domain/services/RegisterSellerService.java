package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.ports.in.RegisterSellerUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterSellerService implements RegisterSellerUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public RegisterSellerService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public Seller execute(Command command) {
        if (command.administratorId() == null) {
            throw new SellerNotAuthorizedException("A seller must be registered by an administrator");
        }
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        Seller seller = new Seller(command.identifier(), command.fullName(), command.email(),
                UserStatus.ACTIVE, SellerStatus.ACTIVE);
        return (Seller) userRepositoryPort.save(seller);
    }
}
