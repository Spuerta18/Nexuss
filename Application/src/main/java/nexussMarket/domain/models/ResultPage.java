package nexussMarket.domain.models;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a longer list: its {@code items}, which page it is, the page
 * size asked for and how many items the whole list has.
 */
public record ResultPage<T>(List<T> items, int page, int size, long totalItems) {

    public ResultPage {
        items = List.copyOf(items);
    }

    /** Number of pages of the whole list. */
    public int totalPages() {
        return (int) ((totalItems + size - 1) / size);
    }

    /** The same page with every item converted by {@code mapper}. */
    public <R> ResultPage<R> map(Function<T, R> mapper) {
        return new ResultPage<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
