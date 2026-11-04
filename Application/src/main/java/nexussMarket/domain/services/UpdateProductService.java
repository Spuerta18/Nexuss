package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeProductOwnershipService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SellerStatus;

/**
 * Changes the name and the price of a product, with the rules of publication:
 * only the owning seller (or an administrator) through
 * {@link AuthorizeProductOwnershipService}, the seller must be {@code ACTIVE},
 * and the price must be greater than zero. A discontinued product cannot be
 * changed (409). Orders already confirmed keep their price.
 */
@Service
public class UpdateProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final AuthorizeProductOwnershipService authorizeProductOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public UpdateProductService(ProductRepositoryPort productRepositoryPort,
                                AuthorizeProductOwnershipService authorizeProductOwnershipService,
                                RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.productRepositoryPort = productRepositoryPort;
        this.authorizeProductOwnershipService = authorizeProductOwnershipService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    /** {@code changes} carries the new name and price; every other field is ignored. */
    public Product execute(User actor, Product changes) {
        String productId = changes.getIdentifier();
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        authorizeProductOwnershipService.execute(actor, product);
        if (product.getSeller() != null && product.getSeller().getSellerStatus() != SellerStatus.ACTIVE) {
            throw new SellerNotAuthorizedException("Seller " + product.getSeller().getIdentifier()
                    + " is not allowed to change products");
        }
        String previousName = product.getName();
        String previousPrice = product.getPrice() != null ? product.getPrice().getAmount().toPlainString() : null;
        product.updateDetails(changes.getName(), changes.getPrice());
        Product saved = productRepositoryPort.save(product);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.PRODUCT_UPDATE, actor, AffectedEntityType.PRODUCT, productId),
                AuditDetails.of("previousName", previousName, "newName", product.getName(),
                        "previousPrice", previousPrice, "newPrice", product.getPrice().getAmount().toPlainString(),
                        "currency", product.getPrice().getCurrency().getCode()));
        return saved;
    }
}
