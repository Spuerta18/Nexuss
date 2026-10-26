package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductStatus;

@Service
public class UpdateProductStatusService {

    private final ProductRepositoryPort productRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;

    public UpdateProductStatusService(ProductRepositoryPort productRepositoryPort,
                                      AuthorizeProductOwnershipService authorizeProductOwnershipService,
                                      RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.productRepositoryPort = productRepositoryPort;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Product execute(User actor, Product productReference, ProductStatus status) {
        String productId = productReference.getIdentifier();
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        authorizeProductOwnershipService.execute(actor, product);
        ProductStatus previousStatus = product.getStatus();
        product.changeStatus(status);
        Product saved = productRepositoryPort.save(product);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.PRODUCT_STATUS_CHANGE, actor, AffectedEntityType.PRODUCT, productId),
                AuditDetails.of("previousStatus", previousStatus.getCode(),
                        "newStatus", product.getStatus().getCode()));
        return saved;
    }
}
