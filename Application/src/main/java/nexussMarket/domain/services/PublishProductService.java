package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.PublishProductUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SellerStatus;

public class PublishProductService implements PublishProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public PublishProductService(ProductRepositoryPort productRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public Product execute(Command command) {
        Seller seller = findSeller(command.sellerId());
        if (seller.getSellerStatus() != SellerStatus.ACTIVE) {
            throw new SellerNotAuthorizedException("Seller " + command.sellerId() + " is not allowed to publish products");
        }
        Product product = new Product(command.identifier(), command.name(), command.productType(), ProductStatus.PUBLISHED);
        product.setSeller(seller);
        product.setVariants(command.variants());
        product.setPrice(command.price());
        return productRepositoryPort.save(product);
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
