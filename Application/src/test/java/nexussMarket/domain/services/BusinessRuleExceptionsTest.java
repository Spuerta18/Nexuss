package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.BuyerNotEnabledException;
import nexussMarket.domain.exceptions.EmptyCartException;
import nexussMarket.domain.exceptions.InactiveWarehouseException;
import nexussMarket.domain.exceptions.InvalidProductPriceException;
import nexussMarket.domain.exceptions.InvalidQuantityException;
import nexussMarket.domain.exceptions.InventoryNotTrackedException;
import nexussMarket.domain.exceptions.OrderNotShippableException;
import nexussMarket.domain.exceptions.OrderStatusRequiresShipmentException;
import nexussMarket.domain.exceptions.ProductNotPurchasableException;
import nexussMarket.domain.exceptions.RefundAlreadyProcessedException;
import nexussMarket.domain.exceptions.ReturnAlreadyDecidedException;
import nexussMarket.domain.exceptions.ReturnNotEligibleException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.InventoryMovementRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Each business rule fails with its own exception, and a rejected operation
 * saves nothing and records no operation or audit log.
 */
class BusinessRuleExceptionsTest extends AuditedServiceTestSupport {

    private final CartRepositoryPort cartRepositoryPort = mock(CartRepositoryPort.class);
    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final ProductRepositoryPort productRepositoryPort = mock(ProductRepositoryPort.class);
    private final UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
    private final WarehouseRepositoryPort warehouseRepositoryPort = mock(WarehouseRepositoryPort.class);
    private final InventoryRepositoryPort inventoryRepositoryPort = mock(InventoryRepositoryPort.class);
    private final ShipmentRepositoryPort shipmentRepositoryPort = mock(ShipmentRepositoryPort.class);
    private final ReturnRequestRepositoryPort returnRequestRepositoryPort = mock(ReturnRequestRepositoryPort.class);

    private final Seller seller = seller();
    private final Administrator admin = administrator();

    @Test
    void confirmingAnEmptyCartFails() {
        Buyer buyer = buyer();
        storeCart(buyer);

        assertThrows(EmptyCartException.class, () -> confirmOrder().execute(buyer, cartReference(), newOrder()));
        assertNothingSaved();
    }

    @Test
    void aSuspendedBuyerCannotConfirmAnOrder() {
        Buyer suspended = new Buyer("B-1", "Ana Buyer", "buyer@example.com", UserStatus.ACTIVE,
                CustomerStatus.SUSPENDED);
        storeCart(suspended, new CartLine(product(seller), 1));

        assertThrows(BuyerNotEnabledException.class,
                () -> confirmOrder().execute(suspended, cartReference(), newOrder()));
        assertNothingSaved();
    }

    @Test
    void aProductNoLongerPublishedCannotBeOrdered() {
        Buyer buyer = buyer();
        Product published = product(seller);
        storeCart(buyer, new CartLine(published, 1));
        published.changeStatus(ProductStatus.SUSPENDED);

        assertThrows(ProductNotPurchasableException.class,
                () -> confirmOrder().execute(buyer, cartReference(), newOrder()));
        assertNothingSaved();
    }

    @Test
    void aProductNeedsAPositivePrice() {
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(seller));
        PublishProductService service = new PublishProductService(productRepositoryPort, userRepositoryPort, audit);

        assertThrows(InvalidProductPriceException.class, () -> service.execute(seller,
                new Product("P-2", "Free", ProductType.PHYSICAL, null, null, cop(0), List.of())));
        assertThrows(InvalidProductPriceException.class, () -> service.execute(seller,
                new Product("P-3", "Unpriced", ProductType.PHYSICAL, null, null, null, List.of())));
        verify(productRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    @Test
    void onlyDeliveredOrdersCanBeReturned() {
        Buyer buyer = buyer();
        when(orderRepositoryPort.findById("O-1")).thenReturn(Optional.of(order(buyer, OrderStatus.PAID)));
        RequestReturnService service = new RequestReturnService(returnRequestRepositoryPort, orderRepositoryPort,
                validateRole, authorizeOrder, audit);

        assertThrows(ReturnNotEligibleException.class, () -> service.execute(buyer,
                new ReturnRequest("R-1", new Order("O-1", null, null), null, "Broken")));
        verify(returnRequestRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    @Test
    void onlyPaidOrdersCanBeShipped() {
        when(shipmentRepositoryPort.findById("SH-1")).thenReturn(Optional.empty());
        when(orderRepositoryPort.findById("O-1"))
                .thenReturn(Optional.of(order(buyer(), OrderStatus.PENDING_PAYMENT)));
        CreateShipmentService service =
                new CreateShipmentService(shipmentRepositoryPort, orderRepositoryPort, validateRole, audit);

        assertThrows(OrderNotShippableException.class,
                () -> service.execute(admin, new Shipment("SH-1", new Order("O-1", null, null))));
        verify(shipmentRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    @Test
    void shippedAndDeliveredAreOnlyReachedThroughTheShipment() {
        AdvanceOrderStatusService service = new AdvanceOrderStatusService(orderRepositoryPort,
                mock(InvoiceRepositoryPort.class), validateRole, audit);

        assertThrows(OrderStatusRequiresShipmentException.class,
                () -> service.execute(admin, new Order("O-1", null, null), OrderStatus.SHIPPED));
        assertThrows(OrderStatusRequiresShipmentException.class,
                () -> service.execute(admin, new Order("O-1", null, null), OrderStatus.DELIVERED));
        assertNothingAudited();
    }

    @Test
    void inactiveWarehousesAndDigitalProductsTakeNoStock() {
        Warehouse inactive = new Warehouse("W-1", "Closed", "2 Dock Street", WarehouseOwnerType.SELLER, seller, false);
        Product digital = new Product("P-9", "E-book", ProductType.DIGITAL, ProductStatus.PUBLISHED, seller, cop(10),
                List.of());
        when(productRepositoryPort.findById("P-1")).thenReturn(Optional.of(product(seller)));
        when(productRepositoryPort.findById("P-9")).thenReturn(Optional.of(digital));
        when(warehouseRepositoryPort.findById("W-1")).thenReturn(Optional.of(inactive));
        when(inventoryRepositoryPort.findByProductIdAndWarehouseId(any(), any())).thenReturn(Optional.empty());
        RegisterInventoryInboundService service = new RegisterInventoryInboundService(inventoryRepositoryPort,
                mock(InventoryMovementRepositoryPort.class), productRepositoryPort, warehouseRepositoryPort,
                authorizeWarehouse, audit);

        assertThrows(InactiveWarehouseException.class, () -> service.execute(seller, product(seller), inactive, 5));
        assertThrows(InventoryNotTrackedException.class, () -> service.execute(seller, digital, inactive, 5));
        verify(inventoryRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    @Test
    void aReturnIsDecidedOnlyOnce() {
        ReturnRequest returnRequest = new ReturnRequest("R-1", order(buyer(), OrderStatus.DELIVERED), buyer(), "Broken");
        returnRequest.reject(admin);

        assertThrows(ReturnAlreadyDecidedException.class, () -> returnRequest.approve(admin));
        assertThrows(ReturnAlreadyDecidedException.class, () -> returnRequest.reject(admin));
    }

    @Test
    void aRefundIsProcessedOnlyOnce() {
        Refund refund = new Refund("REF-1", null, cop(100));
        refund.process();

        assertThrows(RefundAlreadyProcessedException.class, refund::process);
    }

    @Test
    void quantitiesMustBePositive() {
        assertThrows(InvalidQuantityException.class, () -> new CartLine(product(seller), 0));
        assertThrows(InvalidQuantityException.class,
                () -> new InventoryItem(product(seller), warehouse(seller), -1, 0, 0));
    }

    private ConfirmOrderService confirmOrder() {
        return new ConfirmOrderService(cartRepositoryPort, orderRepositoryPort, mock(ReserveInventoryService.class),
                mock(ReleaseInventoryReservationService.class), validateRole, authorizeCart, audit);
    }

    private void storeCart(Buyer buyer, CartLine... lines) {
        when(cartRepositoryPort.findById("C-1"))
                .thenReturn(Optional.of(new ShoppingCart("C-1", buyer, new ArrayList<>(List.of(lines)))));
    }

    private static ShoppingCart cartReference() {
        return new ShoppingCart("C-1", null);
    }

    private static Order newOrder() {
        return new Order("O-1", null, null);
    }

    private void assertNothingSaved() {
        verify(orderRepositoryPort, never()).save(any());
        verify(cartRepositoryPort, never()).deleteById(any());
        assertNothingAudited();
    }
}
