package nexussMarket.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.CurrencyMismatchException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.InvalidQuantityException;
import nexussMarket.domain.exceptions.ProductNotPurchasableException;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.UserStatus;

class ShoppingCartTest {

    private final Buyer buyer = new Buyer("B-1", "Ana Smith", "ana@example.com", UserStatus.ACTIVE,
            CustomerStatus.ENABLED);
    private final ShoppingCart cart = new ShoppingCart("C-1", buyer);

    @Test
    void addLineMergesQuantitiesOfTheSameProduct() {
        Product shirt = product("P-1", ProductStatus.PUBLISHED, Currency.COP);

        cart.addLine(shirt, 2);
        cart.addLine(shirt, 3);

        assertEquals(1, cart.getLines().size());
        assertEquals(5, cart.getLines().get(0).getQuantity());
    }

    @Test
    void addLineRejectsProductsThatAreNotPublished() {
        Product suspended = product("P-1", ProductStatus.SUSPENDED, Currency.COP);

        assertThrows(ProductNotPurchasableException.class, () -> cart.addLine(suspended, 1));
    }

    @Test
    void addLineRejectsADifferentCurrency() {
        cart.addLine(product("P-1", ProductStatus.PUBLISHED, Currency.COP), 1);
        Product inDollars = product("P-2", ProductStatus.PUBLISHED, Currency.USD);

        assertThrows(CurrencyMismatchException.class, () -> cart.addLine(inDollars, 1));
        assertEquals(1, cart.getLines().size());
    }

    @Test
    void addLineRejectsNonPositiveQuantity() {
        Product shirt = product("P-1", ProductStatus.PUBLISHED, Currency.COP);

        assertThrows(InvalidQuantityException.class, () -> cart.addLine(shirt, 0));
        assertTrue(cart.getLines().isEmpty());
    }

    @Test
    void updateLineQuantityReplacesTheQuantity() {
        Product shirt = product("P-1", ProductStatus.PUBLISHED, Currency.COP);
        cart.addLine(shirt, 2);

        cart.updateLineQuantity(shirt, 7);

        assertEquals(7, cart.getLines().get(0).getQuantity());
    }

    @Test
    void updateAndRemoveFailWhenTheProductHasNoLine() {
        Product absent = product("P-9", ProductStatus.PUBLISHED, Currency.COP);

        assertThrows(EntityNotFoundException.class, () -> cart.updateLineQuantity(absent, 1));
        assertThrows(EntityNotFoundException.class, () -> cart.removeLine(absent));
    }

    @Test
    void removeLineRemovesTheProduct() {
        Product shirt = product("P-1", ProductStatus.PUBLISHED, Currency.COP);
        cart.addLine(shirt, 1);

        cart.removeLine(shirt);

        assertTrue(cart.getLines().isEmpty());
    }

    private static Product product(String id, ProductStatus status, Currency currency) {
        return new Product(id, "Product " + id, ProductType.PHYSICAL, status, null,
                Money.of(new BigDecimal("1000"), currency), null);
    }
}
