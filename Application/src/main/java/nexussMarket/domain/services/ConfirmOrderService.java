package nexussMarket.domain.services;

import java.util.ArrayList;
import java.util.List;

import nexussMarket.domain.exceptions.BusinessRuleViolationException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.in.ConfirmOrderUseCase;
import nexussMarket.domain.ports.in.ReleaseInventoryReservationUseCase;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Converts a shopping cart into an order, reserving stock for every line. If
 * any reservation fails, the reservations already made are released, so no
 * stock is left reserved for an order that was never created. The cart is
 * deleted once the order is saved. Only the buyer who owns the cart, or an
 * administrator, may confirm it.
 */
public class ConfirmOrderService implements ConfirmOrderUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ReserveInventoryService reserveInventoryService;
    private final ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public ConfirmOrderService(CartRepositoryPort cartRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                ReserveInventoryService reserveInventoryService,
                                ReleaseInventoryReservationUseCase releaseInventoryReservationUseCase,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                AuthorizeCartAccessService authorizeCartAccessService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.reserveInventoryService = reserveInventoryService;
        this.releaseInventoryReservationUseCase = releaseInventoryReservationUseCase;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
    }

    @Override
    public Order execute(User actor, Command command) {
        // Supervisors may consult carts but never turn them into orders.
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER, SystemRole.ADMINISTRATOR);
        ShoppingCart cart = cartRepositoryPort.findById(command.cartId())
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + command.cartId()));
        authorizeCartAccessService.execute(actor, cart);
        requireBuyerCanPurchase(cart.getBuyer());
        if (cart.getLines().isEmpty()) {
            throw new BusinessRuleViolationException("Shopping cart " + command.cartId() + " is empty");
        }
        for (CartLine cartLine : cart.getLines()) {
            requirePublished(cartLine.getProduct());
        }

        Order order = new Order(command.orderId(), cart.getBuyer(), OrderStatus.PENDING_PAYMENT);
        order.setLines(reserveLines(cart));
        Order saved = orderRepositoryPort.save(order);
        cartRepositoryPort.deleteById(cart.getIdentifier());
        return saved;
    }

    private List<OrderLine> reserveLines(ShoppingCart cart) {
        List<OrderLine> orderLines = new ArrayList<>();
        try {
            for (CartLine cartLine : cart.getLines()) {
                Product product = cartLine.getProduct();
                InventoryItem reservedItem = reserveInventoryService.reserve(product.getIdentifier(), cartLine.getQuantity());
                orderLines.add(new OrderLine(product, product.getName(), cartLine.getQuantity(), product.getPrice(),
                        reservedItem.getWarehouse()));
            }
        } catch (RuntimeException e) {
            orderLines.forEach(this::release);
            throw e;
        }
        return orderLines;
    }

    private void release(OrderLine line) {
        releaseInventoryReservationUseCase.execute(new ReleaseInventoryReservationUseCase.Command(
                line.getProduct().getIdentifier(), line.getWarehouse().getIdentifier(), line.getQuantity()));
    }

    private static void requireBuyerCanPurchase(Buyer buyer) {
        if (buyer.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Buyer " + buyer.getIdentifier() + " is not active");
        }
        if (buyer.getCommercialStatus() != CustomerStatus.ENABLED) {
            throw new BusinessRuleViolationException("Buyer " + buyer.getIdentifier() + " is suspended from purchasing");
        }
    }

    private static void requirePublished(Product product) {
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BusinessRuleViolationException(
                    "Product " + product.getIdentifier() + " is not available for purchase");
        }
    }
}
