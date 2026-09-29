package nexussMarket.domain.services;

import java.util.Optional;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.ports.in.AddCartLineUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.valueobjects.ProductStatus;

public class AddCartLineService implements AddCartLineUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;

    public AddCartLineService(CartRepositoryPort cartRepositoryPort, ProductRepositoryPort productRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
    }

    @Override
    public ShoppingCart execute(Command command) {
        ShoppingCart cart = cartRepositoryPort.findById(command.cartId())
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + command.cartId()));
        Product product = productRepositoryPort.findById(command.productId())
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + command.productId()));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new OperationNotAllowedException("Product " + command.productId() + " is not available for purchase");
        }

        Optional<CartLine> existingLine = cart.getLines().stream()
                .filter(line -> line.getProduct().getIdentifier().equals(command.productId()))
                .findFirst();

        if (existingLine.isPresent()) {
            mergeQuantity(existingLine.get(), command);
        } else {
            cart.getLines().add(newCartLine(product, command));
        }

        return cartRepositoryPort.save(cart);
    }

    private void mergeQuantity(CartLine line, Command command) {
        try {
            line.setQuantity(line.getQuantity() + command.quantity());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quantity for product " + command.productId()
                    + " in cart " + command.cartId() + ": " + e.getMessage(), e);
        }
    }

    private CartLine newCartLine(Product product, Command command) {
        try {
            return new CartLine(product, command.quantity());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quantity for product " + command.productId()
                    + " in cart " + command.cartId() + ": " + e.getMessage(), e);
        }
    }
}
