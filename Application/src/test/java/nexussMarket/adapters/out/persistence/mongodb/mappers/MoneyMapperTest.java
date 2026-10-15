package nexussMarket.adapters.out.persistence.mongodb.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import nexussMarket.adapters.out.persistence.mongodb.documents.MoneyDocument;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;

class MoneyMapperTest {

    @Test
    void amountAndCurrencySurviveRoundTrip() {
        Money money = Money.of(new BigDecimal("12500.50"), Currency.COP);

        MoneyDocument document = MoneyMapper.toDocument(money);
        Money rebuilt = MoneyMapper.toDomain(document);

        assertEquals(new BigDecimal("12500.50"), document.getAmount());
        assertEquals("COP", document.getCurrency());
        assertEquals(money, rebuilt);
    }

    @Test
    void nullMapsToNullInBothDirections() {
        assertNull(MoneyMapper.toDocument(null));
        assertNull(MoneyMapper.toDomain(null));
    }

    @Test
    void unknownCurrencyCodeFails() {
        MoneyDocument document = new MoneyDocument();
        document.setAmount(BigDecimal.TEN);
        document.setCurrency("XXX");

        assertThrows(IllegalStateException.class, () -> MoneyMapper.toDomain(document));
    }
}
