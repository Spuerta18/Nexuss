package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.RegisterWarehouseUseCase;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

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
        if (command.ownerType() == WarehouseOwnerType.SELLER) {
            warehouse.setOwner(findSeller(command.sellerId()));
        }
        return warehouseRepositoryPort.save(warehouse);
    }

    private Seller findSeller(String sellerId) {
        User user = userRepositoryPort.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("No seller found with id " + sellerId));
        if (!(user instanceof Seller seller)) {
            throw new EntityNotFoundException("No seller found with id " + sellerId);
        }
        return seller;
    }
}
