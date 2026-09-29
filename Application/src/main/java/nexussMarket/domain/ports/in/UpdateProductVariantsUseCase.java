package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.ProductVariant;

public interface UpdateProductVariantsUseCase {

    record Command(String productId, List<ProductVariant> variants) {}

    Product execute(User actor, Command command);
}
