package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidProductPriceException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SellerStatus;

@Service
public class PublishProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final UserRepositoryPort userRepositoryPort;

    public PublishProductService(ProductRepositoryPort productRepositoryPort, UserRepositoryPort userRepositoryPort,
                                 RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.productRepositoryPort = productRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Product execute(User actor, Product product) {
        Seller seller = findSeller(actor.getIdentifier());
        if (seller.getSellerStatus() != SellerStatus.ACTIVE) {
            throw new SellerNotAuthorizedException("Seller " + seller.getIdentifier() + " is not allowed to publish products");
        }
        if (product.getPrice() == null || !product.getPrice().isPositive()) {
            throw new InvalidProductPriceException(
                    "Product " + product.getIdentifier() + " must have a price greater than zero");
        }
        Product newProduct = new Product(product.getIdentifier(), product.getName(), product.getProductType(),
                ProductStatus.PUBLISHED, seller, product.getPrice(), product.getVariants());
        Product saved = productRepositoryPort.save(newProduct);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.PRODUCT_PUBLICATION, actor, AffectedEntityType.PRODUCT, newProduct.getIdentifier()),
                AuditDetails.of("newStatus", newProduct.getStatus().getCode(),
                        "price", newProduct.getPrice().getAmount().toPlainString(),
                        "currency", newProduct.getPrice().getCurrency().getCode()));
        return saved;
    }

    private Seller findSeller(String sellerId) {
        User user = userRepositoryPort.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("No seller found with id " + sellerId));
        if (!(user instanceof Seller seller)) {
            throw new EntityNotFoundException("No seller found with id " + sellerId);
        }
        return seller;
    }
}
