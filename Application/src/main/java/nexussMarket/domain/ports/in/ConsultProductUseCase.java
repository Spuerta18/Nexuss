package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;

public interface ConsultProductUseCase {

    record Query(String productId) {}

    Product execute(User actor, Query query);
}
