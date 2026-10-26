package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Registers a warehouse, according to the actor's role: an {@link Administrator}
 * registers Marketplace-owned warehouses, and a {@link Seller} registers
 * Seller-owned warehouses with themselves as owner. Any other combination is
 * rejected.
 */
@Service
public class RegisterWarehouseService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public RegisterWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort, UserRepositoryPort userRepositoryPort,
                                    ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                    RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Warehouse execute(User actor, Warehouse warehouse) {
        validateRoleAuthorizationService.execute(actor, SystemRole.SELLER, SystemRole.ADMINISTRATOR);
        if (actor.getRole() == SystemRole.ADMINISTRATOR && warehouse.getOwnerType() == WarehouseOwnerType.MARKETPLACE) {
            return saveAndRecord(actor, newWarehouse(warehouse, null));
        }
        if (actor.getRole() == SystemRole.SELLER && warehouse.getOwnerType() == WarehouseOwnerType.SELLER
                && isOwnedByActor(warehouse, actor)) {
            return saveAndRecord(actor, newWarehouse(warehouse, findSeller(actor.getIdentifier())));
        }
        throw new OperationNotAllowedException(
                "User " + actor.getIdentifier() + " is not allowed to register warehouse " + warehouse.getIdentifier());
    }

    private Warehouse saveAndRecord(User actor, Warehouse warehouse) {
        Warehouse saved = warehouseRepositoryPort.save(warehouse);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.WAREHOUSE_REGISTRATION, actor, AffectedEntityType.WAREHOUSE, warehouse.getIdentifier()),
                AuditDetails.of("ownerType", warehouse.getOwnerType().getCode(),
                        "ownerId", warehouse.getOwner() != null ? warehouse.getOwner().getIdentifier() : null));
        return saved;
    }

    /** The warehouse to store: always active, with {@code owner} as its owner. */
    private static Warehouse newWarehouse(Warehouse warehouse, Seller owner) {
        return new Warehouse(warehouse.getIdentifier(), warehouse.getName(), warehouse.getAddress(),
                warehouse.getOwnerType(), owner, true);
    }

    /** A seller registers warehouses for themselves only: no owner, or themselves as owner. */
    private static boolean isOwnedByActor(Warehouse warehouse, User actor) {
        return warehouse.getOwner() == null || warehouse.getOwner().getIdentifier().equals(actor.getIdentifier());
    }

    private Seller findSeller(String sellerId) {
        if (sellerId == null) {
            throw new OperationNotAllowedException("A Seller-owned warehouse must reference its owning seller");
        }
        User user = userRepositoryPort.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("No seller found with id " + sellerId));
        if (!(user instanceof Seller seller)) {
            throw new EntityNotFoundException("No seller found with id " + sellerId);
        }
        return seller;
    }
}
