package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

/**
 * Empties the buyer's active cart ({@link ConsultActiveCartService}: buyers
 * only, 404 without a cart). The cart is kept, without lines, and saved with
 * its version. An already empty cart is returned unchanged and nothing is
 * recorded; otherwise the clearing is audited ({@code CART_CLEARING}).
 */
@Service
public class ClearActiveCartService {

    private final ConsultActiveCartService consultActiveCartService;
    private final CartRepositoryPort cartRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public ClearActiveCartService(ConsultActiveCartService consultActiveCartService,
                                  CartRepositoryPort cartRepositoryPort,
                                  RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.consultActiveCartService = consultActiveCartService;
        this.cartRepositoryPort = cartRepositoryPort;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public ShoppingCart execute(User actor) {
        ShoppingCart cart = consultActiveCartService.execute(actor);
        int removedLines = cart.getLines().size();
        if (removedLines == 0) {
            return cart;
        }
        cart.clear();
        ShoppingCart saved = cartRepositoryPort.save(cart);
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.CART_CLEARING, actor, AffectedEntityType.SHOPPING_CART, cart.getIdentifier()),
                AuditDetails.of("removedLines", removedLines));
        return saved;
    }
}
