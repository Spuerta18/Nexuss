package nexussMarket.adapters.out.persistence.memory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.OrderMapper;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.ports.out.OrderRepositoryPort;

/** {@link OrderRepositoryPort} on the in-memory {@code orders} collection (versioned). */
@Repository
@Profile("memory")
public class InMemoryOrderRepositoryAdapter implements OrderRepositoryPort {

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
}
