package nexussMarket.adapters.out.persistence.memory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.OrderMapper;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.PageQuery;

/** {@link OrderRepositoryPort} on the in-memory {@code orders} collection (versioned). */
@Repository
@Profile("memory")
public class InMemoryOrderRepositoryAdapter implements OrderRepositoryPort {

    /** Statuses of an order that is still open: not delivered nor cancelled. */
    private static final Comparator<OrderDocument> MOST_RECENT_FIRST =
            Comparator.comparing(OrderDocument::getCreatedAt).reversed().thenComparing(OrderDocument::getId);

    static final List<String> OPEN_STATUSES = List.of(OrderStatus.CART.getCode(),
            OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.PAID.getCode(), OrderStatus.SHIPPED.getCode());

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryOrderRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Order insert(Order order) {
        database.orders.insert(OrderMapper.toDocument(order),
                "An order with id " + order.getIdentifier() + " already exists");
        return order;
    }

    @Override
    public Order save(Order order) {
        OrderDocument saved = database.orders.save(OrderMapper.toDocument(order), "Order " + order.getIdentifier());
        order.restoreVersion(saved.getVersion());
        return order;
    }

    @Override
    public Optional<Order> findById(String identifier) {
        return database.orders.findById(identifier)
                .map(document -> referenceResolver.toOrders(List.of(document)).get(0));
    }

    /** Orders placed by the buyer, most recent first. */
    @Override
    public List<Order> findAllByBuyerId(String buyerId) {
        return referenceResolver.toOrders(database.orders.find(document -> buyerId.equals(document.getBuyerId()))
                .stream().sorted(Comparator.comparing(OrderDocument::getCreatedAt).reversed()).toList());
    }

    @Override
    public boolean existsOpenOrderWithProduct(String productId) {
        return database.orders.exists(document -> OPEN_STATUSES.contains(document.getStatus())
                && document.getLines().stream().anyMatch(line -> productId.equals(line.getProductId())));
    }

    @Override
    public ResultPage<Order> findPage(PageQuery page) {
        return InMemoryPages.page(database.orders.findAll(), MOST_RECENT_FIRST, page, referenceResolver::toOrders);
    }

    @Override
    public ResultPage<Order> findPageByProductIds(Collection<String> productIds, PageQuery page) {
        return InMemoryPages.page(database.orders.find(document -> document.getLines().stream()
                        .anyMatch(line -> productIds.contains(line.getProductId()))),
                MOST_RECENT_FIRST, page, referenceResolver::toOrders);
    }

    @Override
    public List<Order> findAllCreatedBetween(LocalDateTime from, LocalDateTime to) {
        return referenceResolver.toOrders(database.orders.find(document -> document.getCreatedAt() != null
                && !document.getCreatedAt().isBefore(from) && document.getCreatedAt().isBefore(to)));
    }
}
