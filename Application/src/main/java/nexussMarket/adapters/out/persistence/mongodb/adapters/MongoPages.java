package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.valueobjects.PageQuery;

/** Translates between the domain's {@link PageQuery} / {@link ResultPage} and Spring Data paging. */
final class MongoPages {

    private MongoPages() {
    }

    static Pageable pageable(PageQuery page, Sort sort) {
        return PageRequest.of(page.page(), page.size(), sort);
    }

    /** The domain page of {@code documents}, converting the content with a single call to {@code convert}. */
    static <D, T> ResultPage<T> toResultPage(Page<D> documents, PageQuery page, Function<List<D>, List<T>> convert) {
        return new ResultPage<>(convert.apply(documents.getContent()), page.page(), page.size(),
                documents.getTotalElements());
    }
}
