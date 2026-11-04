package nexussMarket.adapters.out.persistence.mongodb.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.repositories.InventoryItemMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.InventoryMovementMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.OrderMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ProductMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ReturnRequestMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.ShoppingCartMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.UserMongoRepository;
import nexussMarket.adapters.out.persistence.mongodb.repositories.WarehouseMongoRepository;
import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.exceptions.InsufficientStockException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.CartLine;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.OrderLine;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.services.ReserveInventoryService;
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
 * Optimistic locking of the MongoDB adapters against a real server: a stale
 * save or delete fails with {@link ConcurrentUpdateException} and stores
 * nothing; 60 threads reserving 1 unit each of a 10-unit stock get exactly 10
 * reservations; and {@link VersionFieldMigration} brings legacy documents up
 * to date and changes nothing the second time. Uses a throwaway database on
 * {@code 127.0.0.1:27017}, dropped afterwards; skipped without a server.
 */
class MongoVersioningIntegrationTest {

    private static MongoClient client;
    private static MongoTemplate template;
    private static MongoRepositoryFactory repositories;

    private final Seller seller = new Seller("S-1", "Sam Seller", "sam@example.com", UserStatus.ACTIVE,
            SellerStatus.ACTIVE);

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
        template = new MongoTemplate(client, "nexusmarket_version_test_" + UUID.randomUUID().toString().substring(0, 8));
        MongoPersistentEntityIndexResolver indexResolver =
                new MongoPersistentEntityIndexResolver(template.getConverter().getMappingContext());
        indexResolver.resolveIndexFor(UserDocument.class).forEach(template.indexOps(UserDocument.class)::createIndex);
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
    void staleOrderSaveFailsAndStoresNothing() {
        Product product = storedProduct("P-ORD", seller);
        Warehouse warehouse = storedWarehouse("W-ORD", seller);
        Buyer buyer = storedBuyer("B-ORD");
        MongoOrderRepositoryAdapter orders = new MongoOrderRepositoryAdapter(
                repositories.getRepository(OrderMongoRepository.class), resolver());
        orders.insert(new Order("O-1", buyer, OrderStatus.PENDING_PAYMENT,
                List.of(new OrderLine(product, "Keyboard", 1, cop(1000), warehouse))));
        Order first = orders.findById("O-1").orElseThrow();
        Order stale = orders.findById("O-1").orElseThrow();
        first.cancel();
        orders.save(first);

        stale.advanceTo(OrderStatus.PAID);
        assertThrows(ConcurrentUpdateException.class, () -> orders.save(stale));

        Order stored = orders.findById("O-1").orElseThrow();
        assertEquals(OrderStatus.CANCELLED, stored.getStatus());
        assertEquals(1, stored.getVersion());
    }

    @Test
    void openOrdersHoldingAProductAreFoundByTheNestedLineQuery() {
        Product product = storedProduct("P-OPEN", seller);
        Warehouse warehouse = storedWarehouse("W-OPEN", seller);
        Buyer buyer = storedBuyer("B-OPEN");
        MongoOrderRepositoryAdapter orders = new MongoOrderRepositoryAdapter(
                repositories.getRepository(OrderMongoRepository.class), resolver());
        orders.insert(new Order("O-OPEN", buyer, OrderStatus.PENDING_PAYMENT,
                List.of(new OrderLine(product, "Keyboard", 1, cop(1000), warehouse))));

        assertEquals(true, orders.existsOpenOrderWithProduct("P-OPEN"));
        assertEquals(false, orders.existsOpenOrderWithProduct("P-NONE"));
        Order order = orders.findById("O-OPEN").orElseThrow();
        order.cancel();
        orders.save(order);
        assertEquals(false, orders.existsOpenOrderWithProduct("P-OPEN"), "a cancelled order is not open");
    }

    @Test
    void staleCartDeleteFailsAndKeepsTheCart() {
        Product product = storedProduct("P-CART", seller);
        Buyer buyer = storedBuyer("B-CART");
        MongoCartRepositoryAdapter carts = new MongoCartRepositoryAdapter(
                repositories.getRepository(ShoppingCartMongoRepository.class), resolver());
        carts.insert(new ShoppingCart("C-1", buyer, List.of(new CartLine(product, 1))));
        ShoppingCart first = carts.findById("C-1").orElseThrow();
        ShoppingCart stale = carts.findById("C-1").orElseThrow();
        first.updateLineQuantity(product, 3);
        carts.save(first);

        assertThrows(ConcurrentUpdateException.class, () -> carts.delete(stale));

        assertEquals(3, carts.findById("C-1").orElseThrow().getLines().get(0).getQuantity());
        carts.delete(carts.findById("C-1").orElseThrow());
        assertEquals(true, carts.findById("C-1").isEmpty());
    }

    @Test
    void staleUserSaveFailsAndANewUserIsInsertOnly() {
        MongoUserRepositoryAdapter users = new MongoUserRepositoryAdapter(
                repositories.getRepository(UserMongoRepository.class));
        users.insert(new Buyer("B-USR", "Ana Buyer", "ana.usr@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED));
        User first = users.findById("B-USR").orElseThrow();
        User stale = users.findById("B-USR").orElseThrow();
        first.setStatus(UserStatus.BLOCKED);
        users.save(first);

        stale.setFullName("Overwritten");
        assertThrows(ConcurrentUpdateException.class, () -> users.save(stale));
        assertThrows(DuplicateResourceException.class, () -> users.insert(
                new Buyer("B-USR", "Impostor", "other@example.com", UserStatus.ACTIVE, CustomerStatus.ENABLED)));

        User stored = users.findById("B-USR").orElseThrow();
        assertEquals("Ana Buyer", stored.getFullName());
        assertEquals(UserStatus.BLOCKED, stored.getStatus());
    }

    @Test
    void sixtyConcurrentReservationsOfTenUnitsReserveExactlyTen() throws Exception {
        Product product = storedProduct("P-RACE", seller);
        Warehouse warehouse = storedWarehouse("W-RACE", seller);
        MongoInventoryRepositoryAdapter inventory = new MongoInventoryRepositoryAdapter(
                repositories.getRepository(InventoryItemMongoRepository.class), resolver());
        inventory.insert(new InventoryItem(product, warehouse, 10, 0, 0));
        ReserveInventoryService reserve = new ReserveInventoryService(inventory,
                new MongoInventoryMovementRepositoryAdapter(
                        repositories.getRepository(InventoryMovementMongoRepository.class), resolver()));

        ExecutorService pool = Executors.newFixedThreadPool(60);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<RuntimeException>> results = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            results.add(pool.submit(() -> {
                go.await();
                try {
                    reserve.reserve("P-RACE", 1);
                    return null;
                } catch (RuntimeException e) {
                    return e;
                }
            }));
        }
        go.countDown();
        int reserved = 0;
        for (Future<RuntimeException> result : results) {
            RuntimeException failure = result.get(60, TimeUnit.SECONDS);
            if (failure == null) {
                reserved++;
            } else {
                assertInstanceOf(InsufficientStockException.class, failure);
            }
        }
        pool.shutdown();

        assertEquals(10, reserved);
        InventoryItem stored = inventory.findByProductIdAndWarehouseId("P-RACE", "W-RACE").orElseThrow();
        assertEquals(0, stored.getAvailableQuantity());
        assertEquals(10, stored.getReservedQuantity());
    }

    @Test
    void migrationVersionsLegacyDocumentsAndNormalizesEmailsOnce() {
        template.getCollection("orders").insertOne(new Document("_id", "LEGACY-O").append("status", "PAID"));
        template.getCollection("refunds").insertOne(new Document("_id", "LEGACY-R").append("status", "PENDING"));
        template.getCollection("users").insertOne(new Document("_id", "LEGACY-U1").append("userType", "BUYER")
                .append("email", "  Mixed.Case@Example.COM "));
        template.getCollection("users").insertOne(new Document("_id", "LEGACY-U2").append("userType", "BUYER")
                .append("email", "taken@example.com"));
        template.getCollection("users").insertOne(new Document("_id", "LEGACY-U3").append("userType", "BUYER")
                .append("email", "TAKEN@example.com"));
        VersionFieldMigration migration = new VersionFieldMigration(template);

        long versioned = migration.addMissingVersions();
        long normalized = migration.normalizeEmails();

        assertEquals(true, versioned >= 5);
        assertEquals(1, normalized);
        assertEquals(0L, ((Number) stored("orders", "LEGACY-O").get("version")).longValue());
        assertEquals(0L, ((Number) stored("refunds", "LEGACY-R").get("version")).longValue());
        assertEquals("mixed.case@example.com", stored("users", "LEGACY-U1").getString("email"));
        assertEquals(1L, ((Number) stored("users", "LEGACY-U1").get("version")).longValue());
        // The normalized form belongs to another user: left as it was.
        assertEquals("TAKEN@example.com", stored("users", "LEGACY-U3").getString("email"));

        assertEquals(0, migration.addMissingVersions());
        assertEquals(0, migration.normalizeEmails());
    }

    private Product storedProduct(String id, Seller owner) {
        storedSeller(owner);
        Product product = new Product(id, "Keyboard", ProductType.PHYSICAL, ProductStatus.PUBLISHED, owner, cop(1000),
                List.of());
        new MongoProductRepositoryAdapter(repositories.getRepository(ProductMongoRepository.class), resolver())
                .insert(product);
        return product;
    }

    private Warehouse storedWarehouse(String id, Seller owner) {
        storedSeller(owner);
        Warehouse warehouse = new Warehouse(id, "Main", "2 Dock Street", WarehouseOwnerType.SELLER, owner, true);
        new MongoWarehouseRepositoryAdapter(repositories.getRepository(WarehouseMongoRepository.class), resolver())
                .insert(warehouse);
        return warehouse;
    }

    private void storedSeller(Seller owner) {
        MongoUserRepositoryAdapter users = new MongoUserRepositoryAdapter(
                repositories.getRepository(UserMongoRepository.class));
        if (!users.existsById(owner.getIdentifier())) {
            users.insert(owner);
        }
    }

    private Buyer storedBuyer(String id) {
        Buyer buyer = new Buyer(id, "Ana Buyer", id.toLowerCase() + "@example.com", UserStatus.ACTIVE,
                CustomerStatus.ENABLED);
        new MongoUserRepositoryAdapter(repositories.getRepository(UserMongoRepository.class)).insert(buyer);
        return buyer;
    }

    private static Money cop(long amount) {
        return Money.of(BigDecimal.valueOf(amount), Currency.COP);
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
}
