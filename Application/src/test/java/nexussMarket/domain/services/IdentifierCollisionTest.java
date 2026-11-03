package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
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
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Creating an entity with an identifier another owner already uses fails with
 * {@link DuplicateResourceException} (409): the stored entity is never saved
 * over, no stock stays reserved and nothing is audited. The insert-only
 * behavior of the Mongo adapters themselves is covered by
 * {@code MongoInsertOnlyIntegrationTest}.
 */
class IdentifierCollisionTest extends AuditedServiceTestSupport {

    private final CartRepositoryPort cartRepositoryPort = mock(CartRepositoryPort.class);
    private final OrderRepositoryPort orderRepositoryPort = mock(OrderRepositoryPort.class);
    private final ProductRepositoryPort productRepositoryPort = mock(ProductRepositoryPort.class);
    private final WarehouseRepositoryPort warehouseRepositoryPort = mock(WarehouseRepositoryPort.class);
    private final UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
    private final ReserveInventoryService reserveInventoryService = mock(ReserveInventoryService.class);
    private final ReleaseInventoryReservationService releaseService = mock(ReleaseInventoryReservationService.class);

    private final Buyer otherBuyer =
            new Buyer("B-2", "Bea Buyer", "bea@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);

    @Test
    void cartWithAnotherBuyersIdentifierIsRejected() {
        Buyer buyer = buyer();
        when(userRepositoryPort.findById("B-1")).thenReturn(Optional.of(buyer));
        when(cartRepositoryPort.insert(any())).thenThrow(new DuplicateResourceException("taken"));

        assertThrows(DuplicateResourceException.class, () ->
                new CreateShoppingCartService(cartRepositoryPort, userRepositoryPort, audit)
                        .execute(buyer, new ShoppingCart("C-OF-B-2", null)));

        verify(cartRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    @Test
    void orderWithTheIdentifierOfAnotherBuyersDeliveredOrderIsRejectedBeforeReserving() {
        Buyer buyer = buyer();
        Product product = product(seller());
        cartOf(buyer, product);
        when(orderRepositoryPort.findById("O-OF-B-2"))
                .thenReturn(Optional.of(new Order("O-OF-B-2", otherBuyer, OrderStatus.DELIVERED, List.of())));

        assertThrows(DuplicateResourceException.class, () -> confirmOrder().execute(buyer,
                new ShoppingCart("C-1", null), new Order("O-OF-B-2", null, null)));

        verifyNoInteractions(reserveInventoryService, releaseService);
        verify(orderRepositoryPort, never()).insert(any());
        verify(orderRepositoryPort, never()).save(any());
        verify(cartRepositoryPort, never()).deleteById(any());
        assertNothingAudited();
    }

    @Test
    void orderLosingTheInsertRaceReleasesEveryReservation() {
        Buyer buyer = buyer();
        Product product = product(seller());
        Warehouse warehouse = warehouse(seller());
        cartOf(buyer, product);
        when(reserveInventoryService.reserve(eq("P-1"), anyInt()))
                .thenReturn(new InventoryItem(product, warehouse, 8, 2, 0));
        // The identifier was free when checked, but another confirmation stored it first.
        when(orderRepositoryPort.insert(any())).thenThrow(new DuplicateResourceException("taken"));

        assertThrows(DuplicateResourceException.class, () -> confirmOrder().execute(buyer,
                new ShoppingCart("C-1", null), new Order("O-OF-B-2", null, null)));

        verify(releaseService).execute(product, warehouse, 2);
        verify(orderRepositoryPort, never()).save(any());
        verify(cartRepositoryPort, never()).deleteById(any());
        assertNothingAudited();
    }

    @Test
    void productWithAnotherSellersIdentifierIsRejected() {
        Seller seller = seller();
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(seller));
        when(productRepositoryPort.insert(any())).thenThrow(new DuplicateResourceException("taken"));

        assertThrows(DuplicateResourceException.class, () ->
                new PublishProductService(productRepositoryPort, userRepositoryPort, audit).execute(seller,
                        new Product("P-OF-S-2", "Mouse", ProductType.PHYSICAL, null, null, cop(500), List.of())));

        verify(productRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    @Test
    void warehouseWithAnotherSellersIdentifierIsRejected() {
        Seller seller = seller();
        when(userRepositoryPort.findById("S-1")).thenReturn(Optional.of(seller));
        when(warehouseRepositoryPort.insert(any())).thenThrow(new DuplicateResourceException("taken"));

        assertThrows(DuplicateResourceException.class, () ->
                new RegisterWarehouseService(warehouseRepositoryPort, userRepositoryPort, validateRole, audit)
                        .execute(seller, new Warehouse("W-OF-S-2", "North", "3 Dock Street", WarehouseOwnerType.SELLER)));

        verify(warehouseRepositoryPort, never()).save(any());
        assertNothingAudited();
    }

    private ConfirmOrderService confirmOrder() {
        return new ConfirmOrderService(cartRepositoryPort, orderRepositoryPort, reserveInventoryService,
                releaseService, validateRole, authorizeCart, audit);
    }

    private void cartOf(Buyer buyer, Product product) {
        when(cartRepositoryPort.findById("C-1")).thenReturn(Optional.of(
                new ShoppingCart("C-1", buyer, new ArrayList<>(List.of(new CartLine(product, 2))))));
    }
}
