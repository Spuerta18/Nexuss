package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.ports.in.RegisterSellerUseCase;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

public class RegisterSellerService implements RegisterSellerUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public RegisterSellerService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public Seller execute(Command command) {
        requireAdministrator(command.administratorId());
        if (userRepositoryPort.existsById(command.identifier())) {
            throw new DuplicateResourceException("A user with identifier " + command.identifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new DuplicateResourceException("A user with email " + command.email() + " already exists");
        }
        Seller seller = new Seller(command.identifier(), command.fullName(), command.email(),
                UserStatus.ACTIVE, SellerStatus.ACTIVE);
        seller.setPasswordHash(passwordHasherPort.hash(command.password()));
        return (Seller) userRepositoryPort.save(seller);
    }

    private void requireAdministrator(String administratorId) {
        boolean isAdministrator = administratorId != null && userRepositoryPort.findById(administratorId)
                .filter(Administrator.class::isInstance)
                .isPresent();
        if (!isAdministrator) {
            throw new OperationNotAllowedException("A seller must be registered by an administrator");
        }
    }
}
