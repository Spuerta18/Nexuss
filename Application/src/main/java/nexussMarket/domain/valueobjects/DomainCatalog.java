package nexussMarket.domain.valueobjects;

import java.util.List;
import java.util.Objects;

/**
 * Base concept for every business catalog. Represents the common shape of a
 * controlled, named set of allowed values with a {@code code}, a display
 * {@code name}, and a {@code description}.
 *
 * <p>Value Objects are immutable and their equality is determined by value,
 * not by identity.</p>
 */
public abstract class DomainCatalog {

    private final String code;
    private final String name;
    private final String description;

    protected DomainCatalog(String code, String name, String description) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
    }

    /** Unique code identifying the value. */
    public String getCode() {
        return code;
    }

    /** Human-readable name. */
    public String getName() {
        return name;
    }

    /** Explanation of the value's business meaning. */
    public String getDescription() {
        return description;
    }

    /**
     * Resolves the canonical constant with the given {@code code} from
     * {@code values}, or {@code null} if unknown. Persistence mappers must use
     * this so that rebuilt entities hold the same instances as the constants.
     */
    protected static <T extends DomainCatalog> T fromCode(List<T> values, String code) {
        return values.stream()
                .filter(v -> v.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DomainCatalog that = (DomainCatalog) o;
        return code.equals(that.code) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), code, name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + code + ")";
    }
}