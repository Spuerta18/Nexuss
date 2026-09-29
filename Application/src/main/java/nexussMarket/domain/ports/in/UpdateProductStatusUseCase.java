package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.ProductStatus;

public interface UpdateProductStatusUseCase {

    record Command(String productId, ProductStatus status) {}

    Product execute(User actor, Command command);
}
