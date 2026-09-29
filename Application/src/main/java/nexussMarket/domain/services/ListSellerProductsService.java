package nexussMarket.domain.services;

import java.util.List;

import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ListSellerProductsUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/**
 * Lists every product of a seller, whatever its status. Administrators and
 * supervisors may list any seller's products; a seller only its own.
 */
public class ListSellerProductsService implements ListSellerProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public ListSellerProductsService(ProductRepositoryPort productRepositoryPort,
                                     ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.productRepositoryPort = productRepositoryPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    @Override
    public List<Product> execute(User actor, Query query) {
        authorize(actor, query.sellerId());
        return productRepositoryPort.findBySellerId(query.sellerId());
    }

    private void authorize(User actor, String sellerId) {
        validateUserAuthorizationStatusService.execute(actor);
        if (actor instanceof Administrator || actor instanceof Supervisor) {
            return;
        }
        if (actor instanceof Seller && actor.getIdentifier().equals(sellerId)) {
            return;
        }
        throw new OperationNotAllowedException(
                "User " + actor.getIdentifier() + " is not allowed to list the products of seller " + sellerId);
    }
}
