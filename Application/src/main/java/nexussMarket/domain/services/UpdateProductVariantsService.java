package nexussMarket.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductVariant;

@Service
public class UpdateProductVariantsService {

    private final ProductRepositoryPort productRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;

    public UpdateProductVariantsService(ProductRepositoryPort productRepositoryPort,
                                        AuthorizeProductOwnershipService authorizeProductOwnershipService,
                                        RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.productRepositoryPort = productRepositoryPort;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Product execute(User actor, Product productReference, List<ProductVariant> variants) {
        String productId = productReference.getIdentifier();
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        authorizeProductOwnershipService.execute(actor, product);
        int previousVariantCount = product.getVariants().size();
        product.replaceVariants(variants);
        Product saved = productRepositoryPort.save(product);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.PRODUCT_VARIANTS_UPDATE, actor, AffectedEntityType.PRODUCT, productId),
                AuditDetails.of("previousVariantCount", previousVariantCount,
                        "newVariantCount", product.getVariants().size()));
        return saved;
    }
}
