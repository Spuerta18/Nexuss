package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;

/**
 * Adds a product to a buyer's cart. The cart rules (published products only,
 * one line per product, a single currency) are enforced by
 * {@link ShoppingCart#addLine}.
 */
@Service
public class AddCartLineService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public AddCartLineService(CartRepositoryPort cartRepositoryPort, ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            AuthorizeCartAccessService authorizeCartAccessService,
            RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
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
        cart.addLine(product, quantity);
        ShoppingCart saved = cartRepositoryPort.save(cart);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.CART_LINE_ADDITION, actor, AffectedEntityType.SHOPPING_CART, cartId),
                AuditDetails.of("productId", productId, "quantity", quantity));
        return saved;
    }
}
