package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;

public interface ListPublishedProductsUseCase {

    record Query() {}

    List<Product> execute(User actor, Query query);
}
