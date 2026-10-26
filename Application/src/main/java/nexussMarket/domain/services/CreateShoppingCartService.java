package nexussMarket.domain.services;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;

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
        ShoppingCart saved = cartRepositoryPort.save(new ShoppingCart(cart.getIdentifier(), buyer));
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.CART_CREATION, actor, AffectedEntityType.SHOPPING_CART, saved.getIdentifier()),
                AuditDetails.of("buyerId", buyer.getIdentifier()));
        return saved;
    }
}
