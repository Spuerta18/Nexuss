package nexussMarket.adapters.in.rest.responses;

import java.util.List;

/**
 * One page of a list: its {@code items}, the {@code page} number (from 0), the
 * page {@code size}, and the size of the whole list.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
}
