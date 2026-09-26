package nexussMarket.domain.ports.in;

import java.util.List;

import nexussMarket.domain.models.Product;

public interface UpdateProductVariantsUseCase {

    record Command(String productId, List<String> variants) {}

    Product execute(Command command);
}
