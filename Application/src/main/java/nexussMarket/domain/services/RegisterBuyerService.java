package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class RegisterBuyerService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public RegisterBuyerService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    public Buyer execute(Buyer buyer, String password) {
        if (userRepositoryPort.existsById(buyer.getIdentifier())) {
            throw new DuplicateResourceException("A user with identifier " + buyer.getIdentifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(buyer.getEmail())) {
            throw new DuplicateResourceException("A user with email " + buyer.getEmail() + " already exists");
        }
        Buyer newBuyer = new Buyer(buyer.getIdentifier(), buyer.getFullName(), buyer.getEmail(),
                UserStatus.ACTIVE, CustomerStatus.ENABLED);
        newBuyer.setPrimaryAddress(buyer.getPrimaryAddress());
        newBuyer.setPasswordHash(passwordHasherPort.hash(password));
        return (Buyer) userRepositoryPort.save(newBuyer);
    }
}
