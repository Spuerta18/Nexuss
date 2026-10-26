package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class RegisterBuyerService {

    private final UserRepositoryPort userRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final PasswordHasherPort passwordHasherPort;

    public RegisterBuyerService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
                                RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
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
        Buyer saved = (Buyer) userRepositoryPort.save(newBuyer);
        // Self-registration: the new buyer is the one who performs it.
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.BUYER_REGISTRATION, saved, AffectedEntityType.USER, saved.getIdentifier()),
                AuditDetails.of("newStatus", saved.getStatus().getCode()));
        return saved;
    }
}
