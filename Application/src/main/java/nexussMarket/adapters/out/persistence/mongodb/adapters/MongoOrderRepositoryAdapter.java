package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.OrderMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.OrderMongoRepository;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.PageQuery;

/** Implements {@link OrderRepositoryPort} on the {@code orders} collection. */
@Repository
@Profile("!memory")
public class MongoOrderRepositoryAdapter implements OrderRepositoryPort {

    /** Statuses of an order that is still open: not delivered nor cancelled. */
    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("_id"));

    static final List<String> OPEN_STATUSES = List.of(OrderStatus.CART.getCode(),
            OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.PAID.getCode(), OrderStatus.SHIPPED.getCode());

    private final OrderMongoRepository orderRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoOrderRepositoryAdapter(OrderMongoRepository orderRepository, MongoReferenceResolver referenceResolver) {
        this.orderRepository = orderRepository;
        this.referenceResolver = referenceResolver;
    }

    /** A real {@code insert}: an existing document with the same key is never replaced. */
    @Override
    public Order insert(Order order) {
        MongoInsertion.insert(() -> orderRepository.insert(OrderMapper.toDocument(order)),
                "An order with id " + order.getIdentifier() + " already exists");
        return order;
    }

    @Override
    public Order save(Order order) {
        order.restoreVersion(MongoVersioning.save(() -> orderRepository.save(OrderMapper.toDocument(order)),
                "Order " + order.getIdentifier()).getVersion());
        return order;
    }

    @Override
    public Optional<Order> findById(String identifier) {
        return orderRepository.findById(identifier)
                .map(document -> referenceResolver.toOrders(List.of(document)).get(0));
    }

    /** Orders placed by the buyer, most recent first. */
    @Override
    public List<Order> findAllByBuyerId(String buyerId) {
        return referenceResolver.toOrders(orderRepository.findAllByBuyerIdOrderByCreatedAtDesc(buyerId));
    }

    @Override
    public boolean existsOpenOrderWithProduct(String productId) {
        return orderRepository.existsByLinesProductIdAndStatusIn(productId, OPEN_STATUSES);
    }

    @Override
    public ResultPage<Order> findPage(PageQuery page) {
        return MongoPages.toResultPage(orderRepository.findAll(MongoPages.pageable(page, MOST_RECENT_FIRST)), page,
                referenceResolver::toOrders);
    }

    @Override
    public ResultPage<Order> findPageByProductIds(Collection<String> productIds, PageQuery page) {
        if (productIds.isEmpty()) {
            return new ResultPage<>(List.of(), page.page(), page.size(), 0);
        }
        return MongoPages.toResultPage(orderRepository.findByLinesProductIdIn(productIds,
                MongoPages.pageable(page, MOST_RECENT_FIRST)), page, referenceResolver::toOrders);
    }
}
