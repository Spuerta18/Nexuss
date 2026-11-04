package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.InvalidPageRequestException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.SupervisorPort;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

import org.springframework.beans.factory.annotation.Autowired;

/** Read-only paginated lists: who sees what, and the page limits. */
class ListsScenariosTest extends MemoryScenarioSupport {

    private static final PageQuery ALL = new PageQuery(0, PageQuery.MAX_SIZE);

    @Autowired
    private SupervisorPort supervisorPort;

    @Test
    void administratorsAndSupervisorsListUsersByRoleOnePageAtATime() {
        Administrator admin = administrator();
        seller();
        seller();
        seller();

        ResultPage<User> sellers = administratorPort.listUsers(admin, SystemRole.SELLER, new PageQuery(0, 2));

        assertEquals(2, sellers.items().size());
        assertTrue(sellers.totalItems() >= 3);
        assertEquals((int) ((sellers.totalItems() + 1) / 2), sellers.totalPages());
        assertTrue(sellers.items().stream().allMatch(user -> user.getRole() == SystemRole.SELLER));
        ResultPage<User> next = administratorPort.listUsers(admin, SystemRole.SELLER, new PageQuery(1, 2));
        assertFalse(next.items().stream().anyMatch(user -> sellers.items().stream()
                .anyMatch(first -> first.getIdentifier().equals(user.getIdentifier()))), "pages do not overlap");
        assertEquals(sellers.totalItems(),
                supervisorPort.listUsers(supervisor(), SystemRole.SELLER, ALL).totalItems());
        assertThrows(OperationNotAllowedException.class, () -> sellerPort.listOrders(buyerAsSeller(), ALL));
    }

    @Test
    void aSellerSeesOnlyOrdersWithTheirProductsAndOnlyTheirLines() {
        Seller mine = seller();
        Seller other = seller();
        Product myProduct = stockedProduct(mine, warehouseOf(mine), 5);
        Product otherProduct = stockedProduct(other, warehouseOf(other), 5);
        Buyer buyer = buyer();
        buyerPort.addActiveCartLine(buyer, reference(myProduct), 1);
        buyerPort.addActiveCartLine(buyer, reference(otherProduct), 2);
        Order mixed = buyerPort.confirmActiveCart(buyer, new Order(null, null, null));
        Order onlyOther = pendingOrder(buyer(), otherProduct, 1);

        List<Order> seen = sellerPort.listOrders(mine, ALL).items();

        assertEquals(1, seen.size());
        assertEquals(mixed.getIdentifier(), seen.get(0).getIdentifier());
        assertEquals(1, seen.get(0).getLines().size(), "only the seller's own line");
        assertEquals(myProduct.getIdentifier(), seen.get(0).getLines().get(0).getProduct().getIdentifier());
        // Most recent first: the two orders just created are on the first page of the unrestricted lists.
        List<String> seenByLogistics = logisticsOperatorPort.listOrders(logisticsOperator(), ALL).items().stream()
                .map(Order::getIdentifier).toList();
        List<String> seenByAdmin = administratorPort.listOrders(administrator(), ALL).items().stream()
                .map(Order::getIdentifier).toList();
        assertTrue(seenByLogistics.containsAll(List.of(mixed.getIdentifier(), onlyOther.getIdentifier())));
        assertTrue(seenByAdmin.containsAll(List.of(mixed.getIdentifier(), onlyOther.getIdentifier())));
        Order fullMixed = administratorPort.listOrders(administrator(), ALL).items().stream()
                .filter(order -> order.getIdentifier().equals(mixed.getIdentifier())).findFirst().orElseThrow();
        assertEquals(2, fullMixed.getLines().size(), "staff see every line");
    }

    @Test
    void aSellerSeesOnlyTheirWarehousesAndInventory() {
        Seller mine = seller();
        Seller other = seller();
        Warehouse myWarehouse = warehouseOf(mine);
        Product myProduct = stockedProduct(mine, myWarehouse, 3);
        stockedProduct(other, warehouseOf(other), 4);

        List<Warehouse> warehouses = sellerPort.listWarehouses(mine, ALL).items();
        List<InventoryItem> inventory = sellerPort.listInventory(mine, ALL).items();

        assertEquals(List.of(myWarehouse.getIdentifier()), warehouses.stream().map(Warehouse::getIdentifier).toList());
        assertEquals(1, inventory.size());
        assertEquals(myProduct.getIdentifier(), inventory.get(0).getProduct().getIdentifier());
        assertTrue(supervisorPort.listWarehouses(supervisor(), ALL).totalItems() >= 2);
        assertTrue(administratorPort.listInventory(administrator(), ALL).totalItems() >= 2);
    }

    @Test
    void pageNumbersAndSizesOutOfRangeAreRejected() {
        assertThrows(InvalidPageRequestException.class, () -> new PageQuery(-1, 20));
        assertThrows(InvalidPageRequestException.class, () -> new PageQuery(0, 0));
        assertThrows(InvalidPageRequestException.class, () -> new PageQuery(0, 101));
        assertEquals(20, PageQuery.first().size());
        ResultPage<User> beyond = administratorPort.listUsers(administrator(), null, new PageQuery(10_000, 20));
        assertTrue(beyond.items().isEmpty(), "a page past the end is empty, not an error");
    }

    private nexussMarket.domain.models.User supervisor() {
        String id = unique("SUP");
        return users.insert(new nexussMarket.domain.models.Supervisor(id, "Supervisor " + id, id + "@test.local",
                nexussMarket.domain.valueobjects.UserStatus.ACTIVE));
    }

    /** A buyer calling a seller list: the role check of the service rejects it. */
    private User buyerAsSeller() {
        return buyer();
    }
}
