package nexussMarket.domain.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import nexussMarket.domain.exceptions.BuyerNotEnabledException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EmptyCartException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.ProductNotPurchasableException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.services.authorization.AuthorizeCartAccessService;
import nexussMarket.domain.services.authorization.ValidateRoleAuthorizationService;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Converts a shopping cart into an order, reserving stock for every line. If
 * any reservation fails, the reservations already made are released, so no
 * stock is left reserved for an order that was never created. The order is
 * inserted, never saved over an existing one: a taken identifier is rejected
 * before reserving, and if the insert still loses a race every reservation is
 * released; the stored order is left untouched either way. The cart is
 * deleted once the order is stored. Only the buyer who owns the cart, or an
 * administrator, may confirm it. Without an identifier the server generates
 * one ({@code ORD-xxxxxxxx}).
 */
@Service
public class ConfirmOrderService {

    private final CartRepositoryPort cartRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ReserveInventoryService reserveInventoryService;
    private final ReleaseInventoryReservationService releaseInventoryReservationService;
    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;
    private final AuthorizeCartAccessService authorizeCartAccessService;

    public ConfirmOrderService(CartRepositoryPort cartRepositoryPort, OrderRepositoryPort orderRepositoryPort,
                                ReserveInventoryService reserveInventoryService,
                                ReleaseInventoryReservationService releaseInventoryReservationService,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                AuthorizeCartAccessService authorizeCartAccessService,
                                RegisterOperationAndAuditService registerOperationAndAuditService) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.orderRepositoryPort = orderRepositoryPort;
        this.reserveInventoryService = reserveInventoryService;
        this.releaseInventoryReservationService = releaseInventoryReservationService;
        this.validateRoleAuthorizationService = validateRoleAuthorizationService;
        this.authorizeCartAccessService = authorizeCartAccessService;
        this.registerOperationAndAuditService = registerOperationAndAuditService;
    }

    public Order execute(User actor, ShoppingCart cartReference, Order newOrder) {
        String cartId = cartReference.getIdentifier();
        // Supervisors may consult carts but never turn them into orders.
        validateRoleAuthorizationService.execute(actor, SystemRole.BUYER, SystemRole.ADMINISTRATOR);
        ShoppingCart cart = cartRepositoryPort.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("No shopping cart found with id " + cartId));
        authorizeCartAccessService.execute(actor, cart);
        requireBuyerCanPurchase(cart.getBuyer());
        if (cart.getLines().isEmpty()) {
            throw new EmptyCartException("Shopping cart " + cartId + " is empty");
        }
        for (CartLine cartLine : cart.getLines()) {
            requirePublished(cartLine.getProduct());
        }

        // Fast path: a taken identifier is rejected before any stock is reserved. The
        // insert below is still the guarantee when two confirmations race.
        String orderId = GeneratedIdentifiers.requestedOrGenerated(newOrder.getIdentifier(), GeneratedIdentifiers.ORDER,
                identifier -> orderRepositoryPort.findById(identifier).isPresent());
        if (orderRepositoryPort.findById(orderId).isPresent()) {
            throw new DuplicateResourceException("An order with id " + orderId + " already exists");
        }
        List<OrderLine> reservedLines = reserveLines(cart);
        Order order = new Order(orderId, cart.getBuyer(), OrderStatus.PENDING_PAYMENT, reservedLines);
        Order saved;
        try {
            saved = orderRepositoryPort.insert(order);
        } catch (RuntimeException e) {
            reservedLines.forEach(this::release);
            throw e;
        }
        cartRepositoryPort.deleteById(cart.getIdentifier());
        registerOperationAndAuditService.execute(
                Operation.of(OperationType.ORDER_CONFIRMATION, actor, AffectedEntityType.ORDER, order.getIdentifier()),
                AuditDetails.of("cartId", cartId, "newStatus", order.getStatus().getCode(),
                        "lineCount", order.getLines().size()));
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
        releaseInventoryReservationService.execute(line.getProduct(), line.getWarehouse(), line.getQuantity());
    }

    private static void requireBuyerCanPurchase(Buyer buyer) {
        if (buyer.getStatus() != UserStatus.ACTIVE) {
            throw new BuyerNotEnabledException("Buyer " + buyer.getIdentifier() + " is not active");
        }
        if (buyer.getCommercialStatus() != CustomerStatus.ENABLED) {
            throw new BuyerNotEnabledException("Buyer " + buyer.getIdentifier() + " is suspended from purchasing");
        }
    }

    private static void requirePublished(Product product) {
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new ProductNotPurchasableException(
                    "Product " + product.getIdentifier() + " is not available for purchase");
        }
    }
}
