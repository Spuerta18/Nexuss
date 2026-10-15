package nexussMarket.adapters.in.rest.mappers;

import java.util.function.Function;

import nexussMarket.adapters.in.rest.exception.InvalidRequestException;
import nexussMarket.domain.valueobjects.DomainCatalog;

/**
 * Resolves the catalog codes received in requests (e.g. {@code "SUSPENDED"})
 * into their Value Object constants. An unknown code is a client error and
 * fails with {@link InvalidRequestException} ({@code 400}).
 */
final class RestCodes {

    private RestCodes() {
    }

    static <T extends DomainCatalog> T resolve(Function<String, T> fromCode, String code, String field) {
        T value = fromCode.apply(code);
        if (value == null) {
            throw new InvalidRequestException("Unknown " + field + " '" + code + "'");
        }
        return value;
    }

    /** The code of {@code value}, or {@code null}. */
    static String code(DomainCatalog value) {
        return value != null ? value.getCode() : null;
    }
}
