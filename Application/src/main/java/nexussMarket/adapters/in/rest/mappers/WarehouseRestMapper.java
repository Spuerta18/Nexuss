package nexussMarket.adapters.in.rest.mappers;

import nexussMarket.adapters.in.rest.requests.RegisterWarehouseRequest;
import nexussMarket.adapters.in.rest.responses.WarehouseResponse;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/** Converts warehouse requests into domain models and warehouses into {@link WarehouseResponse}. */
public final class WarehouseRestMapper {

    private WarehouseRestMapper() {
    }

    /**
     * A new warehouse with the ownership given by the endpoint. The owner is
     * left empty: for a Seller-owned warehouse the service sets the
     * authenticated seller.
     */
    public static Warehouse newWarehouse(RegisterWarehouseRequest request, WarehouseOwnerType ownerType) {
        return new Warehouse(request.identifier(), request.name(), request.address(), ownerType);
    }

    public static WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getIdentifier(), warehouse.getName(), warehouse.getAddress(),
                RestCodes.code(warehouse.getOwnerType()),
                warehouse.getOwner() != null ? warehouse.getOwner().getIdentifier() : null, warehouse.isActive());
    }
}
