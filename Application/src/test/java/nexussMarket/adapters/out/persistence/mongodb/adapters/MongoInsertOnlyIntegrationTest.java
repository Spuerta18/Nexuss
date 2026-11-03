package nexussMarket.adapters.out.persistence.mongodb.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import nexussMarket.adapters.out.persistence.mongodb.documents.RefundDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ReturnRequestDocument;
import nexussMarket.adapters.out.persistence.mongodb.documents.ShipmentDocument;
import nexussMarket.adapters.out.persistence.mongodb.repositories.OrderMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ProductMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.RefundMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ReturnRequestMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ShipmentMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ShoppingCartMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.UserMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.WarehouseMongoRepository;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.OrderStatus;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Runs the {@code insert} of the Mongo adapters against a real server: a
 * document whose identifier (or unique key) another owner already uses is
 * rejected with {@link DuplicateResourceException} and the stored document is
 * left exactly as it was. Uses a throwaway database on {@code 127.0.0.1:27017}
 * that is dropped afterwards; skipped when no MongoDB server is reachable, so
 * the suite still runs without one.
 */
class MongoInsertOnlyIntegrationTest {

    private static MongoClient client;
    private static MongoTemplate template;
    private static MongoRepositoryFactory repositories;

    private final Buyer firstBuyer =
            new Buyer("B-1", "Ana Buyer", "ana@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);
    private final Buyer secondBuyer =
            new Buyer("B-2", "Bea Buyer", "bea@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED);
    private final Seller firstSeller =
            new Seller("S-1", "Sam Seller", "sam@example.com", UserStatus.ACTIVE, SellerStatus.ACTIVE);
    private final Seller secondSeller =
            new Seller("S-2", "Sol Seller", "sol@example.com", UserStatus.ACTIVE, SellerStatus.ACTIVE);

    @BeforeAll
    static void connect() {
        client = MongoClients.create("mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=1000&connectTimeoutMS=1000");
        try {
            client.getDatabase("admin").runCommand(new Document("ping", 1));
        } catch (MongoException e) {
            client.close();
            client = null;
        }
        assumeTrue(client != null, "No MongoDB server on 127.0.0.1:27017");
        template = new MongoTemplate(client, "nexusmarket_insert_test_" + UUID.randomUUID().toString().substring(0, 8));
        // The unique indexes the application creates at startup (auto-index-creation).
        MongoPersistentEntityIndexResolver indexResolver =
                new MongoPersistentEntityIndexResolver(template.getConverter().getMappingContext());
        for (Class<?> type : List.of(ShipmentDocument.class, ReturnRequestDocument.class, RefundDocument.class)) {
            indexResolver.resolveIndexFor(type).forEach(template.indexOps(type)::createIndex);
        }
        repositories = new MongoRepositoryFactory(template);
    }

    @AfterAll
    static void dropDatabase() {
        if (client != null) {
            template.getDb().drop();
            client.close();
        }
    }

    @Test
    void cartOfAnotherBuyerIsNeverReplaced() {
        MongoCartRepositoryAdapter carts = new MongoCartRepositoryAdapter(
                repositories.getRepository(ShoppingCartMongoRepository.class), resolver());
        carts.insert(new ShoppingCart("C-1", firstBuyer));

        assertThrows(DuplicateResourceException.class, () -> carts.insert(new ShoppingCart("C-1", secondBuyer)));

        assertEquals("B-1", stored("shopping_carts", "C-1").getString("buyerId"));
    }

    @Test
    void deliveredOrderOfAnotherBuyerIsNeverReplaced() {
        MongoOrderRepositoryAdapter orders = orders();
        orders.insert(new Order("O-1", firstBuyer, OrderStatus.DELIVERED, List.of(line())));

        assertThrows(DuplicateResourceException.class, () ->
                orders.insert(new Order("O-1", secondBuyer, OrderStatus.PENDING_PAYMENT, List.of(line()))));

        Document order = stored("orders", "O-1");
        assertEquals("B-1", order.getString("buyerId"));
        assertEquals("DELIVERED", order.getString("status"));
    }

    @Test
    void productOfAnotherSellerIsNeverReplaced() {
        MongoProductRepositoryAdapter products = new MongoProductRepositoryAdapter(
                repositories.getRepository(ProductMongoRepository.class), resolver());
        products.insert(product("P-1", "Keyboard", firstSeller));

        assertThrows(DuplicateResourceException.class, () -> products.insert(product("P-1", "Fake", secondSeller)));

        Document product = stored("products", "P-1");
        assertEquals("S-1", product.getString("sellerId"));
        assertEquals("Keyboard", product.getString("name"));
    }

    @Test
    void warehouseOfAnotherSellerIsNeverReplaced() {
        MongoWarehouseRepositoryAdapter warehouses = new MongoWarehouseRepositoryAdapter(
                repositories.getRepository(WarehouseMongoRepository.class), resolver());
        warehouses.insert(warehouse("W-1", "Main", firstSeller));

        assertThrows(DuplicateResourceException.class, () ->
                warehouses.insert(warehouse("W-1", "Hijacked", secondSeller)));

        Document warehouse = stored("warehouses", "W-1");
        assertEquals("S-1", warehouse.getString("ownerId"));
        assertEquals("Main", warehouse.getString("name"));
    }

    @Test
    void shipmentReturnAndRefundAreInsertOnlyByIdentifierAndByUniqueKey() {
        Order order = new Order("O-9", firstBuyer, OrderStatus.DELIVERED, List.of(line()));
        Order otherOrder = new Order("O-10", secondBuyer, OrderStatus.DELIVERED, List.of(line()));
        MongoShipmentRepositoryAdapter shipments = new MongoShipmentRepositoryAdapter(
                repositories.getRepository(ShipmentMongoRepository.class), resolver());
        MongoReturnRequestRepositoryAdapter returns = new MongoReturnRequestRepositoryAdapter(
                repositories.getRepository(ReturnRequestMongoRepository.class), resolver());
        MongoRefundRepositoryAdapter refunds = new MongoRefundRepositoryAdapter(
                repositories.getRepository(RefundMongoRepository.class), resolver());

        shipments.insert(new Shipment("SH-1", order));
        assertThrows(DuplicateResourceException.class, () -> shipments.insert(new Shipment("SH-1", otherOrder)));
        assertThrows(DuplicateResourceException.class, () -> shipments.insert(new Shipment("SH-2", order)));
        assertEquals("O-9", stored("shipments", "SH-1").getString("orderId"));

        ReturnRequest returnRequest = new ReturnRequest("RR-1", order, firstBuyer, "Broken");
        returns.insert(returnRequest);
        assertThrows(DuplicateResourceException.class, () ->
                returns.insert(new ReturnRequest("RR-1", otherOrder, secondBuyer, "Other")));
        assertThrows(DuplicateResourceException.class, () ->
                returns.insert(new ReturnRequest("RR-2", order, firstBuyer, "Again")));
        assertEquals("O-9", stored("return_requests", "RR-1").getString("orderId"));

        refunds.insert(new Refund("REF-RR-1", returnRequest, cop(2000)));
        assertThrows(DuplicateResourceException.class, () ->
                refunds.insert(new Refund("REF-RR-2", returnRequest, cop(1))));
        assertEquals("RR-1", stored("refunds", "REF-RR-1").getString("returnRequestId"));
    }

    private MongoOrderRepositoryAdapter orders() {
        return new MongoOrderRepositoryAdapter(repositories.getRepository(OrderMongoRepository.class), resolver());
    }

    private static MongoReferenceResolver resolver() {
        return new MongoReferenceResolver(repositories.getRepository(UserMongoRepository.class),
                repositories.getRepository(ProductMongoRepository.class),
                repositories.getRepository(WarehouseMongoRepository.class),
                repositories.getRepository(OrderMongoRepository.class),
                repositories.getRepository(ReturnRequestMongoRepository.class));
    }

    private static Document stored(String collection, String identifier) {
        return template.getCollection(collection).find(new Document("_id", identifier)).first();
    }

    private OrderLine line() {
        Product product = product("P-1", "Keyboard", firstSeller);
        return new OrderLine(product, product.getName(), 2, product.getPrice(), warehouse("W-1", "Main", firstSeller));
    }

    private static Product product(String identifier, String name, Seller seller) {
        return new Product(identifier, name, ProductType.PHYSICAL, ProductStatus.PUBLISHED, seller, cop(1000),
                List.of());
    }

    private static Warehouse warehouse(String identifier, String name, Seller owner) {
        return new Warehouse(identifier, name, "2 Dock Street", WarehouseOwnerType.SELLER, owner, true);
    }

    private static Money cop(long amount) {
        return Money.of(BigDecimal.valueOf(amount), Currency.COP);
    }
}
