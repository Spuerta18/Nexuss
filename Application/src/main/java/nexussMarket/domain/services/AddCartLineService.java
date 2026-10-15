package nexussMarket.domain.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SystemRole;

@Service
public class AddCartLineService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public AddCartLineService(CartRepositoryPort cartRepositoryPort, ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            AuthorizeCartAccessService authorizeCartAccessService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
    }

    public ShoppingCart execute(User actor, ShoppingCart cartReference, Product productReference, int quantity) {
        String cartId = cartReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        String productId = productReference.getIdentifier();
        ShoppingCart cart = cartRepositoryPort.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + cartId));
        authorizeCartAccessService.execute(actor, cart);
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("No product found with id " + productId));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BusinessRuleViolationException("Product " + productId + " is not available for purchase");
        }

        Optional<CartLine> existingLine = cart.getLines().stream()
                .filter(line -> line.getProduct().getIdentifier().equals(productId))
                .findFirst();

        if (existingLine.isPresent()) {
            mergeQuantity(existingLine.get(), cartId, productId, quantity);
        } else {
            cart.getLines().add(newCartLine(product, cartId, quantity));
        }

        return cartRepositoryPort.save(cart);
    }

    private void mergeQuantity(CartLine line, String cartId, String productId, int quantity) {
        try {
            line.setQuantity(line.getQuantity() + quantity);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quantity for product " + productId
                    + " in cart " + cartId + ": " + e.getMessage(), e);
        }
    }

    private CartLine newCartLine(Product product, String cartId, int quantity) {
        try {
            return new CartLine(product, quantity);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quantity for product " + product.getIdentifier()
                    + " in cart " + cartId + ": " + e.getMessage(), e);
        }
    }
}
