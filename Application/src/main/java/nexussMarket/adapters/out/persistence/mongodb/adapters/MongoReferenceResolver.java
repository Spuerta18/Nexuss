package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ProductDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;
import nexussMarket.adapters.out.persistence.mongodb.repositories.OrderMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ProductMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ReturnRequestMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.UserMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.WarehouseMongoRepository;

/**
 * {@link DocumentReferenceResolver} reading the referenced documents from
 * MongoDB: every lookup of several IDs costs a single query per collection.
 */
@Component
@Profile("!memory")
public class MongoReferenceResolver extends DocumentReferenceResolver {

    private final UserMongoRepository userRepository;
    private final ProductMongoRepository productRepository;
    private final WarehouseMongoRepository warehouseRepository;
    private final OrderMongoRepository orderRepository;
    private final ReturnRequestMongoRepository returnRequestRepository;

    public MongoReferenceResolver(UserMongoRepository userRepository, ProductMongoRepository productRepository,
                                  WarehouseMongoRepository warehouseRepository, OrderMongoRepository orderRepository,
                                  ReturnRequestMongoRepository returnRequestRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderRepository = orderRepository;
        this.returnRequestRepository = returnRequestRepository;
    }

    @Override
    protected List<UserDocument> findUsers(List<String> ids) {
        return userRepository.findAllById(ids);
    }

    @Override
    protected List<ProductDocument> findProducts(List<String> ids) {
        return productRepository.findAllById(ids);
    }

    @Override
    protected List<WarehouseDocument> findWarehouses(List<String> ids) {
        return warehouseRepository.findAllById(ids);
    }

    @Override
    protected List<OrderDocument> findOrders(List<String> ids) {
        return orderRepository.findAllById(ids);
    }

    @Override
    protected List<ReturnRequestDocument> findReturnRequests(List<String> ids) {
        return returnRequestRepository.findAllById(ids);
    }
}
