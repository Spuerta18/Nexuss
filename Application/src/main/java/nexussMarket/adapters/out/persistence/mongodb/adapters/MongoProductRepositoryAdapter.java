package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.ProductMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ProductMongoRepository;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.valueobjects.ProductStatus;

/** Implements {@link ProductRepositoryPort} on the {@code products} collection. */
@Repository
public class MongoProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductMongoRepository productRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoProductRepositoryAdapter(ProductMongoRepository productRepository,
                                         MongoReferenceResolver referenceResolver) {
        this.productRepository = productRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Product save(Product product) {
        productRepository.save(ProductMapper.toDocument(product));
        return product;
    }

    @Override
    public Optional<Product> findById(String identifier) {
        return productRepository.findById(identifier).map(referenceResolver::toProduct);
    }

    @Override
    public List<Product> findBySellerId(String sellerId) {
        return referenceResolver.toProducts(productRepository.findBySellerId(sellerId));
    }

    @Override
    public List<Product> findAllPublished() {
        return referenceResolver.toProducts(productRepository.findByStatus(ProductStatus.PUBLISHED.getCode()));
    }
}
