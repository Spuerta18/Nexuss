package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.util.function.Function;

import nexussMarket.domain.valueobjects.DomainCatalog;

/**
 * Resolves stored catalog codes back into their Value Object constants. A code
 * the catalog does not know means corrupt data, so it fails with
 * {@link IllegalStateException} instead of rebuilding the entity with a
 * {@code null} value.
 */
final class CatalogCodes {

    private CatalogCodes() {
    }

    static <T extends DomainCatalog> T resolve(Function<String, T> fromCode, String code, String catalog) {
        T value = fromCode.apply(code);
        if (value == null) {
            throw new IllegalStateException("Unknown " + catalog + " code '" + code + "'");
        }
        return value;
    }
}
