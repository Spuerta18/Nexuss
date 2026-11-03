package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Product;

public interface ProductRepositoryPort {

    /**
     * Stores a new product. Fails with {@link DuplicateResourceException} if a product with the same
     * identifier already exists; the stored one is never replaced.
     */
    Product insert(Product product);

    /** Stores the changes of an existing product. */
    Product save(Product product);

    Optional<Product> findById(String identifier);

    List<Product> findBySellerId(String sellerId);

    List<Product> findAllPublished();
}
