package nexussMarket.domain.services;

import java.util.List;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConsultInventoryUseCase;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/**
 * Returns the stock of a product in every warehouse. Administrators,
 * supervisors and logistics operators may consult any product; a seller only
 * its own products.
 */
public class ConsultInventoryService implements ConsultInventoryUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;

    public ConsultInventoryService(ProductRepositoryPort productRepositoryPort,
                                   InventoryRepositoryPort inventoryRepositoryPort,
                                   ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService,
                                   AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        this.productRepositoryPort = productRepositoryPort;
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
    }

    @Override
    public List<InventoryItem> execute(User actor, Query query) {
        Product product = productRepositoryPort.findById(query.productId())
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + query.productId()));
        authorize(actor, product);
        return inventoryRepositoryPort.findAllByProductId(product.getIdentifier());
    }

    private void authorize(User actor, Product product) {
        if (actor instanceof Supervisor || actor instanceof LogisticsOperator) {
            validateUserAuthorizationStatusService.execute(actor);
            return;
        }
        // Administrators, or the seller who owns the product.
        authorizeProductOwnershipService.execute(actor, product);
    }
}
