package nexussMarket.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * A page of inventory records (stock of a product in a warehouse). Read-only.
 * Administrators and supervisors see every record; a seller sees the records
 * of their own products only.
 */
@Service
public class ListInventoryService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ListInventoryService(InventoryRepositoryPort inventoryRepositoryPort,
                                ProductRepositoryPort productRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
    }

    public ResultPage<InventoryItem> execute(User actor, PageQuery page) {
        validateRoleAuthorizationService.execute(actor, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR,
                SystemRole.SELLER);
        if (actor.getRole() == SystemRole.SELLER) {
            List<String> ownProducts = productRepositoryPort.findBySellerId(actor.getIdentifier()).stream()
                    .map(Product::getIdentifier).toList();
            return inventoryRepositoryPort.findPageByProductIds(ownProducts, page);
        }
        return inventoryRepositoryPort.findPage(page);
    }
}
