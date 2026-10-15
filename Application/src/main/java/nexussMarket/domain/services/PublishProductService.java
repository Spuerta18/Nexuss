package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.SellerNotAuthorizedException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SellerStatus;

@Service
public class PublishProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public PublishProductService(ProductRepositoryPort productRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.productRepositoryPort = productRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    public Product execute(User actor, Product product) {
        Seller seller = findSeller(actor.getIdentifier());
        if (seller.getSellerStatus() != SellerStatus.ACTIVE) {
            throw new SellerNotAuthorizedException("Seller " + seller.getIdentifier() + " is not allowed to publish products");
        }
        Product newProduct = new Product(product.getIdentifier(), product.getName(), product.getProductType(),
                ProductStatus.PUBLISHED);
        newProduct.setSeller(seller);
        newProduct.setVariants(product.getVariants());
        newProduct.setPrice(product.getPrice());
        return productRepositoryPort.save(newProduct);
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
