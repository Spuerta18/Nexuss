package nexussMarket.domain.ports.in;

import java.math.BigDecimal;
import java.util.List;

import nexussMarket.domain.models.Product;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.ProductVariant;

public interface PublishProductUseCase {

    record Command(String identifier, String name, ProductType productType, BigDecimal price, String sellerId,
                    List<ProductVariant> variants) {}

    Product execute(Command command);
}
