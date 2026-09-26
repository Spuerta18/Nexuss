package nexussMarket.domain.ports.in;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.valueobjects.ProductStatus;

public interface UpdateProductStatusUseCase {

    record Command(String productId, ProductStatus status) {}

    Product execute(Command command);
}
