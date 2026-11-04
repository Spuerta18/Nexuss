package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.ProductInOpenOrderException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductStatus;

/**
 * Deletes a product logically: it moves to {@code DISCONTINUED} and is never
 * offered again, while orders, invoices and the audit trail keep pointing to
 * it. Only the owning seller (or an administrator) through
 * {@link AuthorizeProductOwnershipService}. Fails with 409 while an open order
 * (not delivered nor cancelled) holds the product. Idempotent: discontinuing a
 * discontinued product returns it unchanged and records nothing.
 */
@Service
public class DiscontinueProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public DiscontinueProductService(ProductRepositoryPort productRepositoryPort,
                                     OrderRepositoryPort orderRepositoryPort,
                                     AuthorizeProductOwnershipService authorizeProductOwnershipService,
                                     RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.productRepositoryPort = productRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Product execute(User actor, Product productReference) {
        String productId = productReference.getIdentifier();
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        authorizeProductOwnershipService.execute(actor, product);
        if (product.getStatus() == ProductStatus.DISCONTINUED) {
            return product;
        }
        if (orderRepositoryPort.existsOpenOrderWithProduct(productId)) {
            throw new ProductInOpenOrderException("Product " + productId
                    + " is part of an open order; it can be discontinued once that order is delivered or cancelled");
        }
        ProductStatus previousStatus = product.getStatus();
        product.changeStatus(ProductStatus.DISCONTINUED);
        Product saved = productRepositoryPort.save(product);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.PRODUCT_DISCONTINUATION, actor, AffectedEntityType.PRODUCT, productId),
                AuditDetails.of("previousStatus", previousStatus.getCode(), "newStatus", saved.getStatus().getCode()));
        return saved;
    }
}
