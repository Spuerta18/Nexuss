package nexussMarket.adapters.out.persistence.memory;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import nexussMarket.adapters.out.persistence.mongodb.adapters.DocumentReferenceResolver;
import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ProductDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;

/** {@code DocumentReferenceResolver} reading the referenced documents from the {@link InMemoryDatabase}. */
@Component
@Profile("memory")
public class InMemoryReferenceResolver extends DocumentReferenceResolver {

    private final InMemoryDatabase database;

    public InMemoryReferenceResolver(InMemoryDatabase database) {
        this.database = database;
    }

    @Override
    protected List<UserDocument> findUsers(List<String> ids) {
        return database.users.findAllById(ids);
    }

    @Override
    protected List<ProductDocument> findProducts(List<String> ids) {
        return database.products.findAllById(ids);
    }

    @Override
    protected List<WarehouseDocument> findWarehouses(List<String> ids) {
        return database.warehouses.findAllById(ids);
    }

    @Override
    protected List<OrderDocument> findOrders(List<String> ids) {
        return database.orders.findAllById(ids);
    }

    @Override
    protected List<ReturnRequestDocument> findReturnRequests(List<String> ids) {
        return database.returnRequests.findAllById(ids);
    }
}
