package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;

@Service
public class RemoveCartLineService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeCartAccessService authorizeCartAccessService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public RemoveCartLineService(CartRepositoryPort cartRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            AuthorizeCartAccessService authorizeCartAccessService,
            RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public ShoppingCart execute(User actor, ShoppingCart cartReference, Product productReference) {
        String cartId = cartReference.getIdentifier();
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER);
        ShoppingCart cart = cartRepositoryPort.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + cartId));
        authorizeCartAccessService.execute(actor, cart);
        cart.removeLine(productReference);
        ShoppingCart saved = cartRepositoryPort.save(cart);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.CART_LINE_REMOVAL, actor, AffectedEntityType.SHOPPING_CART, cartId),
                AuditDetails.of("productId", productReference.getIdentifier()));
        return saved;
    }
}
