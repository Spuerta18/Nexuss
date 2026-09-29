package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.RegisterWarehouseUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Registers a warehouse. Marketplace-owned warehouses can only be registered
 * by an {@link Administrator}; Seller-owned warehouses must reference their
 * owning {@link Seller}.
 */
public class RegisterWarehouseService implements RegisterWarehouseUseCase {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public RegisterWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public Warehouse execute(Command command) {
        Warehouse warehouse = new Warehouse(command.identifier(), command.name(), command.address(), command.ownerType());
        if (command.ownerType() == WarehouseOwnerType.MARKETPLACE) {
            requireAdministrator(command.administratorId());
        } else {
            warehouse.setOwner(findSeller(command.sellerId()));
        }
        return warehouseRepositoryPort.save(warehouse);
    }

    private void requireAdministrator(String administratorId) {
        boolean isAdministrator = administratorId != null && userRepositoryPort.findById(administratorId)
                .filter(Administrator.class::isInstance)
                .isPresent();
        if (!isAdministrator) {
            throw new OperationNotAllowedException("A Marketplace-owned warehouse must be registered by an administrator");
        }
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
