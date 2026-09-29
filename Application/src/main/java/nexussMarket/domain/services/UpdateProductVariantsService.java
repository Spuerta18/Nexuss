package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.UpdateProductVariantsUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;

public class UpdateProductVariantsService implements UpdateProductVariantsUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;

    public UpdateProductVariantsService(ProductRepositoryPort productRepositoryPort,
                                        AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        this.productRepositoryPort = productRepositoryPort;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
    }

    @Override
    public Product execute(User actor, Command command) {
        Product product = productRepositoryPort.findById(command.productId())
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + command.productId()));
        authorizeProductOwnershipService.execute(actor, product);
        product.setVariants(command.variants());
        return productRepositoryPort.save(product);
    }
}
