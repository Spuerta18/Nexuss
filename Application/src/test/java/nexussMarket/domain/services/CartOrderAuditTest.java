package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.OrderStatus;

/** Cart and order changes record their operation and audit log. */
class CartOrderAuditTest extends AuditedServiceTestSupport {

    private final CartRepositoryPort cartRepositoryPort = mock(CartRepositoryPort.class);
    private final ProductRepositoryPort productRepositoryPort = mock(ProductRepositoryPort.class);
    private final UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final InvoiceRepositoryPort invoiceRepositoryPort = mock(InvoiceRepositoryPort.class);
    private final ReserveInventoryService reserveInventoryService = mock(ReserveInventoryService.class);
    private final ReleaseInventoryReservationService releaseService = mock(ReleaseInventoryReservationService.class);

    private final Buyer buyer = buyer();
    private final Product product = product(seller());

    @BeforeEach
    void repositoriesReturnWhatTheyStore() {
        when(cartRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(orderRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(invoiceRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(productRepositoryPort.findById("P-1")).thenReturn(Optional.of(product));
        when(userRepositoryPort.findById("B-1")).thenReturn(Optional.of(buyer));
    }

    @Test
    void cartCreationIsRecorded() {
        new CreateShoppingCartService(cartRepositoryPort, userRepositoryPort, audit)
                .execute(buyer, new ShoppingCart("C-1", null));

        AuditLog entry = assertAudited(OperationType.CART_CREATION, buyer, AffectedEntityType.SHOPPING_CART, "C-1");
        assertEquals("B-1", entry.getDetails().get("buyerId"));
    }

    @Test
    void cartLineAdditionIsRecorded() {
        cart();

        new AddCartLineService(cartRepositoryPort, productRepositoryPort, validateRole, authorizeCart, audit)
                .execute(buyer, new ShoppingCart("C-1", null), product, 2);

        AuditLog entry = assertAudited(OperationType.CART_LINE_ADDITION, buyer, AffectedEntityType.SHOPPING_CART,
                "C-1");
        assertEquals("P-1", entry.getDetails().get("productId"));
        assertEquals(2, entry.getDetails().get("quantity"));
    }

    @Test
    void cartLineQuantityUpdateIsRecorded() {
        cart(new CartLine(product, 1));

        new UpdateCartLineQuantityService(cartRepositoryPort, validateRole, authorizeCart, audit)
                .execute(buyer, new ShoppingCart("C-1", null), product, 5);

        AuditLog entry = assertAudited(OperationType.CART_LINE_QUANTITY_UPDATE, buyer,
                AffectedEntityType.SHOPPING_CART, "C-1");
        assertEquals(5, entry.getDetails().get("newQuantity"));
    }

    @Test
    void cartLineRemovalIsRecorded() {
        cart(new CartLine(product, 1));

        new RemoveCartLineService(cartRepositoryPort, validateRole, authorizeCart, audit)
                .execute(buyer, new ShoppingCart("C-1", null), product);

        assertAudited(OperationType.CART_LINE_REMOVAL, buyer, AffectedEntityType.SHOPPING_CART, "C-1");
    }

    @Test
    void orderConfirmationIsRecorded() {
        cart(new CartLine(product, 2));
        when(reserveInventoryService.reserve(eq("P-1"), anyInt()))
                .thenReturn(new InventoryItem(product, warehouse(seller()), 8, 2, 0));

        new ConfirmOrderService(cartRepositoryPort, orderRepositoryPort, reserveInventoryService, releaseService,
                validateRole, authorizeCart, audit)
                .execute(buyer, new ShoppingCart("C-1", null), new Order("O-1", null, null));

        AuditLog entry = assertAudited(OperationType.ORDER_CONFIRMATION, buyer, AffectedEntityType.ORDER, "O-1");
        assertEquals("C-1", entry.getDetails().get("cartId"));
        assertEquals("PENDING_PAYMENT", entry.getDetails().get("newStatus"));
        assertEquals(1, entry.getDetails().get("lineCount"));
    }

    @Test
    void orderCancellationRecordsPreviousAndNewStatus() {
        when(orderRepositoryPort.findById("O-1")).thenReturn(Optional.of(order(buyer, OrderStatus.PENDING_PAYMENT)));

        new CancelOrderService(orderRepositoryPort, releaseService, validateRole, authorizeOrder, audit)
                .execute(buyer, new Order("O-1", null, null));

        AuditLog entry = assertAudited(OperationType.ORDER_CANCELLATION, buyer, AffectedEntityType.ORDER, "O-1");
        assertEquals("PENDING_PAYMENT", entry.getDetails().get("previousStatus"));
        assertEquals("CANCELLED", entry.getDetails().get("newStatus"));
    }

    @Test
    void orderStatusAdvanceRecordsStatusesAndInvoice() {
        LogisticsOperator operator = logisticsOperator();
        when(orderRepositoryPort.findById("O-1")).thenReturn(Optional.of(order(buyer, OrderStatus.PENDING_PAYMENT)));

        new AdvanceOrderStatusService(orderRepositoryPort, invoiceRepositoryPort, validateRole, audit)
                .execute(operator, new Order("O-1", null, null), OrderStatus.PAID);

        AuditLog entry = assertAudited(OperationType.ORDER_STATUS_ADVANCE, operator, AffectedEntityType.ORDER, "O-1");
        assertEquals("PENDING_PAYMENT", entry.getDetails().get("previousStatus"));
        assertEquals("PAID", entry.getDetails().get("newStatus"));
        assertEquals("INV-O-1", entry.getDetails().get("invoiceId"));
    }

    private void cart(CartLine... lines) {
        when(cartRepositoryPort.findById("C-1"))
                .thenReturn(Optional.of(new ShoppingCart("C-1", buyer, new ArrayList<>(List.of(lines)))));
    }
}
