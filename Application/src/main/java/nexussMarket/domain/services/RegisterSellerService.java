package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class RegisterSellerService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public RegisterSellerService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    public Seller execute(User actor, Seller seller, String password) {
        requireAdministrator(actor);
        if (userRepositoryPort.existsById(seller.getIdentifier())) {
            throw new DuplicateResourceException("A user with identifier " + seller.getIdentifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(seller.getEmail())) {
            throw new DuplicateResourceException("A user with email " + seller.getEmail() + " already exists");
        }
        Seller newSeller = new Seller(seller.getIdentifier(), seller.getFullName(), seller.getEmail(),
                UserStatus.ACTIVE, SellerStatus.ACTIVE);
        newSeller.setPasswordHash(passwordHasherPort.hash(password));
        return (Seller) userRepositoryPort.save(newSeller);
    }

    private void requireAdministrator(User actor) {
        boolean isAdministrator = actor != null && userRepositoryPort.findById(actor.getIdentifier())
                .filter(Administrator.class::isInstance)
                .isPresent();
        if (!isAdministrator) {
            throw new OperationNotAllowedException("A seller must be registered by an administrator");
        }
    }
}
