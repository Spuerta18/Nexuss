package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Currency a {@link Money} amount is expressed in. Codes follow ISO 4217.
 */
public final class Currency extends DomainCatalog {

    public static final Currency COP =
            new Currency("COP", "Colombian Peso", "Official currency of Colombia.");
    public static final Currency USD =
            new Currency("USD", "US Dollar", "Official currency of the United States.");

    private Currency(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code Currency} value list. */
    public static List<Currency> values() {
        return List.of(COP, USD);
    }

    /** Resolves a {@code Currency} from its code, or {@code null} if unknown. */
    public static Currency fromCode(String code) {
        return fromCode(values(), code);
    }
}
