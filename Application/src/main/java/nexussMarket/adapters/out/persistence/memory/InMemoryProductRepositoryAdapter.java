package nexussMarket.adapters.out.persistence.memory;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.ProductMapper;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.valueobjects.ProductStatus;

/** {@link ProductRepositoryPort} on the in-memory {@code products} collection. */
@Repository
@Profile("memory")
public class InMemoryProductRepositoryAdapter implements ProductRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryProductRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Product insert(Product product) {
        database.products.insert(ProductMapper.toDocument(product),
                "A product with id " + product.getIdentifier() + " already exists");
        return product;
    }

    @Override
    public Product save(Product product) {
        database.products.save(ProductMapper.toDocument(product), "Product " + product.getIdentifier());
        return product;
    }

    @Override
    public Optional<Product> findById(String identifier) {
        return database.products.findById(identifier).map(referenceResolver::toProduct);
    }

    @Override
    public List<Product> findBySellerId(String sellerId) {
        return referenceResolver.toProducts(database.products.find(document -> sellerId.equals(document.getSellerId())));
    }

    @Override
    public List<Product> findAllPublished() {
        return referenceResolver.toProducts(database.products.find(
                document -> ProductStatus.PUBLISHED.getCode().equals(document.getStatus())));
    }
}
