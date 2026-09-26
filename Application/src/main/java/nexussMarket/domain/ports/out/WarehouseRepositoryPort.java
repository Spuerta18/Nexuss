package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.Warehouse;

public interface WarehouseRepositoryPort {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(String identifier);
}
