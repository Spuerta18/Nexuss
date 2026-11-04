package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * A page of warehouses. Read-only. Administrators and supervisors see every
 * warehouse; a seller sees only their own.
 */
@Service
public class ListWarehousesService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ListWarehousesService(WarehouseRepositoryPort warehouseRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public ResultPage<Warehouse> execute(User actor, PageQuery page) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR,
                SystemRole.SELLER);
        if (actor.getRole() == SystemRole.SELLER) {
            return warehouseRepositoryPort.findPageByOwnerId(actor.getIdentifier(), page);
        }
        return warehouseRepositoryPort.findPage(page);
    }
}
