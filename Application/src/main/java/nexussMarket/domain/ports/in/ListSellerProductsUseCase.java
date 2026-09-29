package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;

public interface ListSellerProductsUseCase {

    record Query(String sellerId) {}

    List<Product> execute(User actor, Query query);
}
