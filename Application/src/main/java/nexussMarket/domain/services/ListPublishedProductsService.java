package nexussMarket.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;

/** Lists the public catalog: every {@code PUBLISHED} product. Any active user may consult it. */
@Service
public class ListPublishedProductsService {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;

    public ListPublishedProductsService(ProductRepositoryPort productRepositoryPort,
                                        ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService) {
        this.productRepositoryPort = productRepositoryPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
    }

    public List<Product> execute(User actor) {
        validateUserAuthorizationStatusService.execute(actor);
        return productRepositoryPort.findAllPublished();
    }
}
