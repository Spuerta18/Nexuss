package nexussMarket.adapters.out.persistence.memory;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.valueobjects.PageQuery;

/** Cuts one page out of documents read from the in-memory store, like the MongoDB adapters page a query. */
final class InMemoryPages {

    private InMemoryPages() {
    }

    static <D, T> ResultPage<T> page(List<D> documents, Comparator<D> order, PageQuery page,
                                     Function<List<D>, List<T>> convert) {
        List<D> sorted = documents.stream().sorted(order).toList();
        int from = (int) Math.min(page.offset(), sorted.size());
        int to = Math.min(from + page.size(), sorted.size());
        return new ResultPage<>(convert.apply(sorted.subList(from, to)), page.page(), page.size(), sorted.size());
    }
}
