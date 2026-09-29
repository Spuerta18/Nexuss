package nexussMarket.domain.ports.out;

import java.util.List;
import java.util.Optional;

import nexussMarket.domain.models.Product;

public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findById(String identifier);

    List<Product> findBySellerId(String sellerId);

    List<Product> findAllPublished();
}
