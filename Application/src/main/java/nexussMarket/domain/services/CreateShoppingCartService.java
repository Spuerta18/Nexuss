package nexussMarket.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/**
 * Creates the active cart of the authenticated buyer. A buyer has at most one
 * cart: while one exists (it is deleted when it becomes an order) a new one is
 * rejected with {@link DuplicateResourceException}, and the unique
 * {@code buyerId} of the stored carts enforces it when two creations race.
 * Without an identifier the server generates one ({@code CART-xxxxxxxx}).
 */
@Service
public class CreateShoppingCartService {

    private final CartRepositoryPort cartRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final UserRepositoryPort userRepositoryPort;

    public CreateShoppingCartService(CartRepositoryPort cartRepositoryPort, UserRepositoryPort userRepositoryPort,
                                     RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public ShoppingCart execute(User actor, ShoppingCart cart) {
        User user = userRepositoryPort.findById(actor.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException("No buyer found with id " + actor.getIdentifier()));
        if (!(user instanceof Buyer buyer)) {
            throw new EntityNotFoundException("No buyer found with id " + actor.getIdentifier());
        }
        List<ShoppingCart> activeCarts = cartRepositoryPort.findByBuyerId(buyer.getIdentifier());
        if (!activeCarts.isEmpty()) {
            throw new DuplicateResourceException("Buyer " + buyer.getIdentifier() + " already has an active cart "
                    + activeCarts.get(0).getIdentifier());
        }
        String cartId = GeneratedIdentifiers.requestedOrGenerated(cart.getIdentifier(), GeneratedIdentifiers.CART,
                identifier -> cartRepositoryPort.findById(identifier).isPresent());
        ShoppingCart saved = cartRepositoryPort.insert(new ShoppingCart(cartId, buyer));
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.CART_CREATION, actor, AffectedEntityType.SHOPPING_CART, saved.getIdentifier()),
                AuditDetails.of("buyerId", buyer.getIdentifier()));
        return saved;
    }
}
