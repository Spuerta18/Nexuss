package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.valueobjects.ProductStatus;

@Service
public class UpdateProductStatusService {

    private final ProductRepositoryPort productRepositoryPort;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;

    public UpdateProductStatusService(ProductRepositoryPort productRepositoryPort,
                                      AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        this.productRepositoryPort = productRepositoryPort;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
    }

    public Product execute(User actor, Product productReference, ProductStatus status) {
        String productId = productReference.getIdentifier();
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        authorizeProductOwnershipService.execute(actor, product);
        product.changeStatus(status);
        return productRepositoryPort.save(product);
    }
}
