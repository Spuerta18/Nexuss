package nexussMarket.domain.valueobjects;

import nexussMarket.domain.exceptions.InvalidPageRequestException;

/**
 * Which page of a list to read: {@code page} counts from 0 and {@code size}
 * is between 1 and {@value #MAX_SIZE} ({@value #DEFAULT_SIZE} by default).
 * Anything else fails with {@link InvalidPageRequestException}.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidPageRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPageRequestException("size must be between 1 and " + MAX_SIZE);
        }
    }

    /** The first page with the default size. */
    public static PageQuery first() {
        return new PageQuery(0, DEFAULT_SIZE);
    }

    /** Position of the first element of this page in the whole list. */
    public long offset() {
        return (long) page * size;
    }
}
