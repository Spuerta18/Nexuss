package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

@Service
public class RegisterSellerService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public RegisterSellerService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Seller execute(User actor, Seller seller, String password) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR);
        if (userRepositoryPort.existsById(seller.getIdentifier())) {
            throw new DuplicateResourceException("A user with identifier " + seller.getIdentifier() + " already exists");
        }
        if (userRepositoryPort.existsByEmail(seller.getEmail())) {
            throw new DuplicateResourceException("A user with email " + seller.getEmail() + " already exists");
        }
        Seller newSeller = new Seller(seller.getIdentifier(), seller.getFullName(), seller.getEmail(),
                UserStatus.ACTIVE, SellerStatus.ACTIVE);
        newSeller.setPasswordHash(passwordHasherPort.hash(password));
        Seller saved = (Seller) userRepositoryPort.save(newSeller);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.SELLER_REGISTRATION, actor, AffectedEntityType.USER, saved.getIdentifier()),
                AuditDetails.of("newStatus", saved.getStatus().getCode()));
        return saved;
    }
}
