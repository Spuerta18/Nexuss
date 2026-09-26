package nexussMarket.domain.services;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.ports.in.UpdateProductStatusUseCase;
import nexussMarket.domain.ports.out.ProductRepositoryPort;

public class UpdateProductStatusService implements UpdateProductStatusUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    public UpdateProductStatusService(ProductRepositoryPort productRepositoryPort) {
        this.productRepositoryPort = productRepositoryPort;
    }

    @Override
    public Product execute(Command command) {
        Product product = productRepositoryPort.findById(command.productId())
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + command.productId()));
        product.setStatus(command.status());
        return productRepositoryPort.save(product);
    }
}
