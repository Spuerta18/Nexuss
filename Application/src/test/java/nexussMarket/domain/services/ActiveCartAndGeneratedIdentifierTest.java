package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * The buyer's single active cart (consult, add with implicit creation, update,
 * remove, confirm) and the identifiers the server generates when a client
 * creates a cart, order, product or warehouse without one.
 */
class ActiveCartAndGeneratedIdentifierTest extends AuditedServiceTestSupport {

    private final CartRepositoryPort cartRepositoryPort = mock(CartRepositoryPort.class);
    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final ProductRepositoryPort productRepositoryPort = mock(ProductRepositoryPort.class);
    private final WarehouseRepositoryPort warehouseRepositoryPort = mock(WarehouseRepositoryPort.class);
    private final UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
    private final ReserveInventoryService reserveInventoryService = mock(ReserveInventoryService.class);
    private final ReleaseInventoryReservationService releaseService = mock(ReleaseInventoryReservationService.class);

    private final Buyer buyer = buyer();
    private final Product product = product(seller());

    private final CreateShoppingCartService createCart =
            new CreateShoppingCartService(cartRepositoryPort, userRepositoryPort, audit);
    private final AddCartLineService addLine =
            new AddCartLineService(cartRepositoryPort, productRepositoryPort, validateRole, authorizeCart, audit);
    private final ConsultActiveCartService consultActiveCart =
            new ConsultActiveCartService(cartRepositoryPort, validateRole);
    private final ConfirmOrderService confirmOrder = new ConfirmOrderService(cartRepositoryPort, orderRepositoryPort,
            reserveInventoryService, releaseService, validateRole, authorizeCart, audit);

    @BeforeEach
    void repositoriesReturnWhatTheyStore() {
        when(cartRepositoryPort.insert(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(cartRepositoryPort.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(orderRepositoryPort.insert(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(productRepositoryPort.insert(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(warehouseRepositoryPort.insert(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(productRepositoryPort.findById("P-1")).thenReturn(Optional.of(product));
        when(userRepositoryPort.findById("B-1")).thenReturn(Optional.of(buyer));
    }

    @Test
    void consultActiveCartReturnsTheBuyersOnlyCart() {
        ShoppingCart cart = new ShoppingCart("C-1", buyer);
        when(cartRepositoryPort.findByBuyerId("B-1")).thenReturn(List.of(cart));

        assertSame(cart, consultActiveCart.execute(buyer));
    }

    @Test
    void consultActiveCartFailsWhenTheBuyerHasNone() {
        assertThrows(EntityNotFoundException.class, () -> consultActiveCart.execute(buyer));
    }

    @Test
    void activeCartIsForBuyersOnly() {
        assertThrows(OperationNotAllowedException.class, () -> consultActiveCart.execute(seller()));
        assertThrows(OperationNotAllowedException.class, () -> addActiveLine().execute(administrator(), product, 1));
        verifyNoInteractions(cartRepositoryPort);
    }

    @Test
    void addingTheFirstItemCreatesTheActiveCartWithAGeneratedIdentifier() {
        ShoppingCart created = addActiveLineCreatingTheCart();

        assertTrue(created.getIdentifier().matches("CART-[0-9a-f]{8}"), created.getIdentifier());
        assertEquals(1, created.getLines().size());
        assertSame(buyer, created.getBuyer());
        ArgumentCaptor<Operation> operations = ArgumentCaptor.forClass(Operation.class);
        verify(operationRepositoryPort, times(2)).save(operations.capture());
        assertEquals(List.of(OperationType.CART_CREATION, OperationType.CART_LINE_ADDITION),
                operations.getAllValues().stream().map(Operation::getOperationType).toList());
    }

    @Test
    void addingAnItemUsesTheExistingActiveCart() {
        ShoppingCart cart = new ShoppingCart("C-1", buyer);
        when(cartRepositoryPort.findByBuyerId("B-1")).thenReturn(List.of(cart));
        when(cartRepositoryPort.findById("C-1")).thenReturn(Optional.of(cart));

        ShoppingCart result = addActiveLine().execute(buyer, product, 2);

        assertEquals("C-1", result.getIdentifier());
        verify(cartRepositoryPort, never()).insert(any());
        assertAudited(OperationType.CART_LINE_ADDITION, buyer, AffectedEntityType.SHOPPING_CART, "C-1");
    }

    @Test
    void addingAnItemWhileAnotherRequestCreatesTheCartUsesThatCart() {
        ShoppingCart concurrent = new ShoppingCart("C-9", buyer);
        // Empty when looked up, then the concurrent cart exists when the insert collides.
        when(cartRepositoryPort.findByBuyerId("B-1")).thenReturn(List.of(), List.of(), List.of(concurrent));
        when(cartRepositoryPort.insert(any())).thenThrow(new DuplicateResourceException("taken"));
        when(cartRepositoryPort.findById("C-9")).thenReturn(Optional.of(concurrent));

        ShoppingCart result = addActiveLine().execute(buyer, product, 1);

        assertEquals("C-9", result.getIdentifier());
        assertEquals(1, result.getLines().size());
    }

    @Test
    void updateAndRemoveOnlyTouchTheActorsActiveCart() {
        Buyer other = new Buyer("B-2", "Bea Buyer", "bea@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);
        ShoppingCart othersCart = new ShoppingCart("C-2", other, new ArrayList<>(List.of(new CartLine(product, 1))));
        when(cartRepositoryPort.findByBuyerId("B-2")).thenReturn(List.of(othersCart));
        when(cartRepositoryPort.findById("C-2")).thenReturn(Optional.of(othersCart));

        UpdateCartLineQuantityService update =
                new UpdateCartLineQuantityService(cartRepositoryPort, validateRole, authorizeCart, audit);
        RemoveCartLineService remove = new RemoveCartLineService(cartRepositoryPort, validateRole, authorizeCart, audit);

        // B-1 has no active cart: B-2's cart is never reached.
        assertThrows(EntityNotFoundException.class, () ->
                new UpdateActiveCartLineQuantityService(consultActiveCart, update).execute(buyer, product, 4));
        assertThrows(EntityNotFoundException.class, () ->
                new RemoveActiveCartLineService(consultActiveCart, remove).execute(buyer, product));
        verify(cartRepositoryPort, never()).save(any());
        assertEquals(1, othersCart.getLines().get(0).getQuantity());

        ShoppingCart updated = new UpdateActiveCartLineQuantityService(consultActiveCart, update)
                .execute(other, product, 4);
        assertEquals(4, updated.getLines().get(0).getQuantity());
        ShoppingCart emptied = new RemoveActiveCartLineService(consultActiveCart, remove).execute(other, product);
        assertTrue(emptied.getLines().isEmpty());
    }

    @Test
    void confirmingTheActiveCartGeneratesTheOrderIdentifierAndKeepsTheTotal() {
        ShoppingCart cart = new ShoppingCart("C-1", buyer, new ArrayList<>(List.of(new CartLine(product, 3))));
        when(cartRepositoryPort.findByBuyerId("B-1")).thenReturn(List.of(cart));
        when(cartRepositoryPort.findById("C-1")).thenReturn(Optional.of(cart));
        when(reserveInventoryService.reserve(eq("P-1"), anyInt()))
                .thenReturn(new InventoryItem(product, warehouse(seller()), 7, 3, 0));

        Order order = new ConfirmActiveCartService(consultActiveCart, confirmOrder)
                .execute(buyer, new Order(null, null, null));

        assertTrue(order.getIdentifier().matches("ORD-[0-9a-f]{8}"), order.getIdentifier());
        assertEquals(cop(3000), order.getTotal().orElseThrow());
        verify(cartRepositoryPort).deleteById("C-1");
    }

    @Test
    void confirmingWithoutAnActiveCartReservesNothing() {
        assertThrows(EntityNotFoundException.class, () ->
                new ConfirmActiveCartService(consultActiveCart, confirmOrder).execute(buyer, new Order(null, null, null)));
        verifyNoInteractions(reserveInventoryService, orderRepositoryPort);
        assertNothingAudited();
    }

    @Test
    void aBuyerCannotCreateASecondCart() {
        when(cartRepositoryPort.findByBuyerId("B-1")).thenReturn(List.of(new ShoppingCart("C-1", buyer)));

        DuplicateResourceException error = assertThrows(DuplicateResourceException.class, () ->
                createCart.execute(buyer, new ShoppingCart("C-2", null)));

        assertEquals("Buyer B-1 already has an active cart C-1", error.getMessage());
        verify(cartRepositoryPort, never()).insert(any());
        assertNothingAudited();
    }

    @Test
    void productAndWarehouseWithoutIdentifierGetGeneratedOnes() {
        Seller seller = seller();
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(seller));

        Product published = new PublishProductService(productRepositoryPort, userRepositoryPort, audit).execute(seller,
                new Product(null, "Mug", ProductType.PHYSICAL, null, null,
                        cop(15000), List.of()));
        Warehouse registered = new RegisterWarehouseService(warehouseRepositoryPort, userRepositoryPort, validateRole,
                audit).execute(seller, new Warehouse(null, "North", "3 Dock Street", WarehouseOwnerType.SELLER));

        assertTrue(published.getIdentifier().matches("PRD-[0-9a-f]{8}"), published.getIdentifier());
        assertTrue(registered.getIdentifier().matches("WH-[0-9a-f]{8}"), registered.getIdentifier());
    }

    @Test
    void aGeneratedIdentifierSkipsOneAlreadyInUse() {
        // Every candidate but the first is free; the first one is reported as taken.
        List<String> checked = new ArrayList<>();
        String identifier = GeneratedIdentifiers.requestedOrGenerated(null, GeneratedIdentifiers.ORDER, candidate -> {
            checked.add(candidate);
            return checked.size() == 1;
        });

        assertEquals(2, checked.size());
        assertEquals(checked.get(1), identifier);
        assertEquals("O-1", GeneratedIdentifiers.requestedOrGenerated("O-1", GeneratedIdentifiers.ORDER, c -> true));
    }

    private AddActiveCartLineService addActiveLine() {
        return new AddActiveCartLineService(cartRepositoryPort, createCart, addLine, validateRole);
    }

    /** Adds the first item: the cart the service inserts is then found by its identifier. */
    private ShoppingCart addActiveLineCreatingTheCart() {
        Map<String, ShoppingCart> stored = new HashMap<>();
        when(cartRepositoryPort.insert(any())).thenAnswer(invocation -> {
            ShoppingCart inserted = invocation.getArgument(0);
            stored.put(inserted.getIdentifier(), inserted);
            return inserted;
        });
        when(cartRepositoryPort.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.<String>getArgument(0))));
        return addActiveLine().execute(buyer, product, 2);
    }
}
