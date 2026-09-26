package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.ports.in.UpdateProductVariantsUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;

public class UpdateProductVariantsService implements UpdateProductVariantsUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    public UpdateProductVariantsService(ProductRepositoryPort productRepositoryPort) {
        this.productRepositoryPort = productRepositoryPort;
    }

    @Override
    public Product execute(Command command) {
        Product product = productRepositoryPort.findById(command.productId())
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + command.productId()));
        product.setVariants(command.variants());
        return productRepositoryPort.save(product);
    }
}
