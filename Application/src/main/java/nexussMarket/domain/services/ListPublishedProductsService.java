package nexussMarket.domain.services;

import java.util.List;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ListPublishedProductsUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/** Lists the public catalog: every {@code PUBLISHED} product. Any active user may consult it. */
public class ListPublishedProductsService implements ListPublishedProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public ListPublishedProductsService(ProductRepositoryPort productRepositoryPort,
                                        ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.productRepositoryPort = productRepositoryPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    @Override
    public List<Product> execute(User actor, Query query) {
        validateUserAuthorizationStatusService.execute(actor);
        return productRepositoryPort.findAllPublished();
    }
}
