package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import nexussMarket.adapters.out.persistence.mongodb.documents.OrderDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ProductDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.WarehouseDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.OrderMapper;
import nexussMarket.adapters.out.persistence.mongodb.mappers.ProductMapper;
import nexussMarket.adapters.out.persistence.mongodb.mappers.ReturnRequestMapper;
import nexussMarket.adapters.out.persistence.mongodb.mappers.UserMapper;
import nexussMarket.adapters.out.persistence.mongodb.mappers.WarehouseMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.OrderMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ProductMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ReturnRequestMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.UserMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.WarehouseMongoRepository;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;

/**
 * Resolves the IDs stored in documents into fully built domain entities, so
 * adapters return complete object graphs. Products are resolved with their
 * seller, warehouses with their owner, orders with their buyer and the
 * products and warehouses of their lines, and return requests with their
 * order, buyer and deciding user. Every lookup of several IDs costs a single
 * query per collection.
 *
 * <p>A referenced document that does not exist is a data integrity error and
 * fails with {@link IllegalStateException}.</p>
 */
@Component
public class MongoReferenceResolver {

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

    /** The user with {@code id}, which must be of {@code type}. */
    public <T extends User> T user(String id, Class<T> type) {
        return users(List.of(id), type).get(id);
    }

    /** The users with the given {@code ids}, by identifier; all of them must be of {@code type}. */
    public <T extends User> Map<String, T> users(Collection<String> ids, Class<T> type) {
        List<String> distinctIds = distinct(ids);
        Map<String, T> users = new HashMap<>();
        if (distinctIds.isEmpty()) {
            return users;
        }
        for (UserDocument document : userRepository.findAllById(distinctIds)) {
            User user = UserMapper.toDomain(document);
            if (!type.isInstance(user)) {
                throw new IllegalStateException("User " + document.getId() + " is referenced as a "
                        + type.getSimpleName() + " but its userType is " + document.getUserType());
            }
            users.put(document.getId(), type.cast(user));
        }
        return requireAll(distinctIds, users, "user");
    }

    /** The product with {@code id}. */
    public Product product(String id) {
        return products(List.of(id)).get(id);
    }

    /** The products with the given {@code ids}, by identifier. */
    public Map<String, Product> products(Collection<String> ids) {
        List<String> distinctIds = distinct(ids);
        if (distinctIds.isEmpty()) {
            return new HashMap<>();
        }
        return requireAll(distinctIds, byIdentifier(toProducts(productRepository.findAllById(distinctIds)),
                Product::getIdentifier), "product");
    }

    /** Builds {@code document} into a product, resolving its seller. */
    public Product toProduct(ProductDocument document) {
        return toProducts(List.of(document)).get(0);
    }

    /** Builds {@code documents} into products, resolving their sellers with a single query. */
    public List<Product> toProducts(List<ProductDocument> documents) {
        Map<String, Seller> sellers = users(documents.stream().map(ProductDocument::getSellerId).toList(), Seller.class);
        return documents.stream()
                .map(document -> ProductMapper.toDomain(document, sellers.get(document.getSellerId())))
                .toList();
    }

    /** The warehouse with {@code id}. */
    public Warehouse warehouse(String id) {
        return warehouses(List.of(id)).get(id);
    }

    /** The warehouses with the given {@code ids}, by identifier. */
    public Map<String, Warehouse> warehouses(Collection<String> ids) {
        List<String> distinctIds = distinct(ids);
        if (distinctIds.isEmpty()) {
            return new HashMap<>();
        }
        return requireAll(distinctIds, byIdentifier(toWarehouses(warehouseRepository.findAllById(distinctIds)),
                Warehouse::getIdentifier), "warehouse");
    }

    /** Builds {@code document} into a warehouse, resolving its owner. */
    public Warehouse toWarehouse(WarehouseDocument document) {
        return toWarehouses(List.of(document)).get(0);
    }

    /** Builds {@code documents} into warehouses, resolving their owners with a single query. */
    public List<Warehouse> toWarehouses(List<WarehouseDocument> documents) {
        Map<String, Seller> owners = users(documents.stream().map(WarehouseDocument::getOwnerId).toList(), Seller.class);
        return documents.stream()
                .map(document -> WarehouseMapper.toDomain(document, owners.get(document.getOwnerId())))
                .toList();
    }

    /** The order with {@code id}. */
    public Order order(String id) {
        return orders(List.of(id)).get(id);
    }

    /** The orders with the given {@code ids}, by identifier. */
    public Map<String, Order> orders(Collection<String> ids) {
        List<String> distinctIds = distinct(ids);
        if (distinctIds.isEmpty()) {
            return new HashMap<>();
        }
        return requireAll(distinctIds, byIdentifier(toOrders(orderRepository.findAllById(distinctIds)),
                Order::getIdentifier), "order");
    }

    /**
     * Builds {@code documents} into orders, resolving their buyers and the products
     * and warehouses of every line with a single query per collection.
     */
    public List<Order> toOrders(List<OrderDocument> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<String, Buyer> buyers = users(documents.stream().map(OrderDocument::getBuyerId).toList(), Buyer.class);
        Map<String, Product> products = products(
                documents.stream().flatMap(document -> OrderMapper.productIds(document).stream()).toList());
        Map<String, Warehouse> warehouses = warehouses(
                documents.stream().flatMap(document -> OrderMapper.warehouseIds(document).stream()).toList());
        return documents.stream()
                .map(document -> OrderMapper.toDomain(document, buyers.get(document.getBuyerId()), products,
                        warehouses))
                .toList();
    }

    /** The return request with {@code id}. */
    public ReturnRequest returnRequest(String id) {
        List<String> ids = List.of(id);
        return requireAll(ids, byIdentifier(toReturnRequests(returnRequestRepository.findAllById(ids)),
                ReturnRequest::getIdentifier), "return request").get(id);
    }

    /**
     * Builds {@code documents} into return requests, resolving their orders, buyers
     * and deciding users with a single query per collection. The deciding user may
     * be of any role.
     */
    public List<ReturnRequest> toReturnRequests(List<ReturnRequestDocument> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<String, Order> orders = orders(documents.stream().map(ReturnRequestDocument::getOrderId).toList());
        Map<String, Buyer> buyers = users(documents.stream().map(ReturnRequestDocument::getBuyerId).toList(),
                Buyer.class);
        Map<String, User> deciders = users(documents.stream().map(ReturnRequestDocument::getDecidedById).toList(),
                User.class);
        return documents.stream()
                .map(document -> ReturnRequestMapper.toDomain(document, orders.get(document.getOrderId()),
                        buyers.get(document.getBuyerId()), deciders.get(document.getDecidedById())))
                .toList();
    }

    private static List<String> distinct(Collection<String> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static <T> Map<String, T> byIdentifier(List<T> entities, Function<T, String> identifier) {
        return entities.stream().collect(Collectors.toMap(identifier, Function.identity(), (a, b) -> a, HashMap::new));
    }

    private static <T> Map<String, T> requireAll(List<String> ids, Map<String, T> found, String type) {
        for (String id : ids) {
            if (!found.containsKey(id)) {
                throw new IllegalStateException("Dangling reference: no " + type + " found with id " + id);
            }
        }
        return found;
    }
}
