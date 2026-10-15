package nexussMarket.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.valueobjects.ProductVariant;

@Service
public class UpdateProductVariantsService {

    private final ProductRepositoryPort productRepositoryPort;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;

    public UpdateProductVariantsService(ProductRepositoryPort productRepositoryPort,
                                        AuthorizeProductOwnershipService authorizeProductOwnershipService) {
        this.productRepositoryPort = productRepositoryPort;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
    }

    public Product execute(User actor, Product productReference, List<ProductVariant> variants) {
        String productId = productReference.getIdentifier();
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        authorizeProductOwnershipService.execute(actor, product);
        product.replaceVariants(variants);
        return productRepositoryPort.save(product);
    }
}
