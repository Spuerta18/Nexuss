package nexussMarket.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.DomainException;

class MoneyTest {

    @Test
    void rejectsNullAmount() {
        assertThrows(DomainException.class, () -> Money.of(null, Currency.COP));
    }

    @Test
    void rejectsNullCurrency() {
        assertThrows(DomainException.class, () -> Money.of(BigDecimal.ONE, null));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(DomainException.class, () -> Money.of(new BigDecimal("-0.01"), Currency.COP));
    }

    @Test
    void zeroIsZeroAndNotPositive() {
        Money zero = Money.zero(Currency.COP);

        assertTrue(zero.isZero());
        assertFalse(zero.isPositive());
        assertTrue(Money.of(BigDecimal.ONE, Currency.COP).isPositive());
    }

    @Test
    void addsAndSubtractsInTheSameCurrency() {
        Money ten = Money.of(BigDecimal.TEN, Currency.COP);
        Money three = Money.of(new BigDecimal("3"), Currency.COP);

        assertEquals(Money.of(new BigDecimal("13"), Currency.COP), ten.add(three));
        assertEquals(Money.of(new BigDecimal("7"), Currency.COP), ten.subtract(three));
    }

    @Test
    void subtractRejectsNegativeResult() {
        Money three = Money.of(new BigDecimal("3"), Currency.COP);

        assertThrows(DomainException.class, () -> three.subtract(Money.of(BigDecimal.TEN, Currency.COP)));
    }

    @Test
    void addAndSubtractRejectDifferentCurrency() {
        Money pesos = Money.of(BigDecimal.TEN, Currency.COP);
        Money dollars = Money.of(BigDecimal.ONE, Currency.USD);

        assertThrows(DomainException.class, () -> pesos.add(dollars));
        assertThrows(DomainException.class, () -> pesos.subtract(dollars));
    }

    @Test
    void equalityIgnoresScale() {
        Money a = Money.of(new BigDecimal("10.0"), Currency.COP);
        Money b = Money.of(new BigDecimal("10.00"), Currency.COP);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void sameAmountInDifferentCurrencyIsNotEqual() {
        assertNotEquals(Money.of(BigDecimal.TEN, Currency.COP), Money.of(BigDecimal.TEN, Currency.USD));
    }
}
