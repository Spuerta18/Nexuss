package nexussMarket.adapters.out.persistence.mongodb.mappers;

import nexussMarket.adapters.out.persistence.mongodb.documents.MoneyDocument;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.Money;

/**
 * Converts {@link Money} to and from the embedded {@link MoneyDocument}. A
 * {@code null} value maps to {@code null} in both directions; an unknown
 * currency code is corrupt data and fails like any other catalog code.
 */
final class MoneyMapper {

    private MoneyMapper() {
    }

    static MoneyDocument toDocument(Money money) {
        if (money == null) {
            return null;
        }
        MoneyDocument document = new MoneyDocument();
        document.setAmount(money.getAmount());
        document.setCurrency(money.getCurrency().getCode());
        return document;
    }

    static Money toDomain(MoneyDocument document) {
        if (document == null) {
            return null;
        }
        return Money.of(document.getAmount(),
                CatalogCodes.resolve(Currency::fromCode, document.getCurrency(), "Currency"));
    }
}
