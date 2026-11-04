package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.OrderMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.OrderMongoRepository;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.ports.out.OrderRepositoryPort;

/** Implements {@link OrderRepositoryPort} on the {@code orders} collection. */
@Repository
@Profile("!memory")
public class MongoOrderRepositoryAdapter implements OrderRepositoryPort {

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
}
