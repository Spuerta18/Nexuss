package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConsultProductUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.services.authorization.ValidateUserAuthorizationStatusService;
import nexussMarket.domain.valueobjects.ProductStatus;

/**
 * Returns a single product. Any active user may consult a {@code PUBLISHED}
 * product; any other status is only visible to an administrator or the
 * owning seller.
 */
public class ConsultProductService implements ConsultProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;

    public ConsultProductService(ProductRepositoryPort productRepositoryPort,
                                 ValidateUserAuthorizationStatusService validateUserAuthorizationStatusService,
                                 AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        this.productRepositoryPort = productRepositoryPort;
        this.validateUserAuthorizationStatusService = validateUserAuthorizationStatusService;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
    }

    @Override
    public Product execute(User actor, Query query) {
        validateUserAuthorizationStatusService.execute(actor);
        Product product = productRepositoryPort.findById(query.productId())
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + query.productId()));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            // Suspended or discontinued products are out of the public catalog.
            authorizeProductOwnershipService.execute(actor, product);
        }
        return product;
    }
}
