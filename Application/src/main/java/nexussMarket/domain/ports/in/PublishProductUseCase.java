package nexussMarket.domain.ports.in;

import java.math.BigDecimal;
import java.util.List;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.valueobjects.ProductType;

public interface PublishProductUseCase {

    record Command(String identifier, String name, ProductType productType, BigDecimal price, String sellerId,
                    List<String> variants) {}

    Product execute(Command command);
}
