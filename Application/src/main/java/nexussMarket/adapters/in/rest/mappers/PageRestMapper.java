package nexussMarket.adapters.in.rest.mappers;

import java.util.function.Function;

import nexussMarket.adapters.in.rest.responses.PageResponse;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.valueobjects.PageQuery;

/** Page parameters in, page responses out. */
public final class PageRestMapper {

    private PageRestMapper() {
    }

    /** {@code page} from 0, {@code size} 1 to 100; out of range answers {@code 400}. */
    public static PageQuery query(int page, int size) {
        return new PageQuery(page, size);
    }

    public static <T, R> PageResponse<R> toResponse(ResultPage<T> page, Function<T, R> mapper) {
        ResultPage<R> mapped = page.map(mapper);
        return new PageResponse<>(mapped.items(), mapped.page(), mapped.size(), mapped.totalItems(),
                mapped.totalPages());
    }
}
