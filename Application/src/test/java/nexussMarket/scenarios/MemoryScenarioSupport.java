package nexussMarket.scenarios;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Order;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Refund;
import nexussMarket.domain.models.ReturnRequest;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Shipment;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.in.AdministratorPort;
import nexussMarket.domain.ports.in.BuyerPort;
import nexussMarket.domain.ports.in.LogisticsOperatorPort;
import nexussMarket.domain.ports.in.SellerPort;
import nexussMarket.domain.ports.out.CartRepositoryPort;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.InvoiceRepositoryPort;
import nexussMarket.domain.ports.out.OrderRepositoryPort;
import nexussMarket.domain.ports.out.RefundRepositoryPort;
import nexussMarket.domain.ports.out.ReturnRequestRepositoryPort;
import nexussMarket.domain.ports.out.ShipmentRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Runs the real role ports, Domain Services and in-memory adapters (profile
 * {@code memory}, no database). Builds the data a scenario needs through the
 * ports, with fresh identifiers per call, so tests sharing the context never
 * see each other's data. Users are stored directly (no BCrypt) to keep setup
 * fast; they act exactly like registered ones.
 */
@SpringBootTest
@ActiveProfiles("memory")
public abstract class MemoryScenarioSupport {

    @Autowired
    protected BuyerPort buyerPort;
    @Autowired
    protected SellerPort sellerPort;
    @Autowired
    protected AdministratorPort administratorPort;
    @Autowired
    protected LogisticsOperatorPort logisticsOperatorPort;

    @Autowired
    protected UserRepositoryPort users;
    @Autowired
    protected InventoryRepositoryPort inventory;
    @Autowired
    protected OrderRepositoryPort orders;
    @Autowired
    protected CartRepositoryPort carts;
    @Autowired
    protected InvoiceRepositoryPort invoices;
    @Autowired
    protected ShipmentRepositoryPort shipments;
    @Autowired
    protected ReturnRequestRepositoryPort returnRequests;
    @Autowired
    protected RefundRepositoryPort refunds;

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    protected Administrator administrator() {
        String id = unique("ADM");
        return (Administrator) users.insert(new Administrator(id, "Admin " + id, id + "@test.local", UserStatus.ACTIVE));
    }

    protected Seller seller() {
        String id = unique("SEL");
        return (Seller) users.insert(new Seller(id, "Seller " + id, id + "@test.local", UserStatus.ACTIVE,
                SellerStatus.ACTIVE));
    }

    protected Buyer buyer() {
        String id = unique("BUY");
        Buyer buyer = new Buyer(id, "Buyer " + id, id + "@test.local", UserStatus.ACTIVE, CustomerStatus.ENABLED);
        buyer.setPrimaryAddress("1 Main Street");
        return (Buyer) users.insert(buyer);
    }

    protected LogisticsOperator logisticsOperator() {
        String id = unique("LOG");
        return (LogisticsOperator) users.insert(new LogisticsOperator(id, "Operator " + id, id + "@test.local",
                UserStatus.ACTIVE));
    }

    protected static Money cop(long amount) {
        return Money.of(BigDecimal.valueOf(amount), Currency.COP);
    }

    protected Warehouse warehouseOf(Seller seller) {
        return sellerPort.registerWarehouse(seller,
                new Warehouse(null, "Main", "2 Dock Street", WarehouseOwnerType.SELLER, seller, true));
    }

    /** A published physical product of {@code seller} with {@code stock} units in {@code warehouse}. */
    protected Product stockedProduct(Seller seller, Warehouse warehouse, int stock) {
        Product product = sellerPort.publishProduct(seller, new Product(null, "Keyboard", ProductType.PHYSICAL,
                ProductStatus.PUBLISHED, seller, cop(1000), List.of()));
        if (stock > 0) {
            sellerPort.registerInventoryInbound(seller, reference(product), warehouse, stock);
        }
        return product;
    }

    protected InventoryItem stockOf(Product product, Warehouse warehouse) {
        return inventory.findByProductIdAndWarehouseId(product.getIdentifier(), warehouse.getIdentifier())
                .orElseThrow();
    }

    /** A {@code PENDING_PAYMENT} order of {@code buyer} for {@code quantity} units of {@code product}. */
    protected Order pendingOrder(Buyer buyer, Product product, int quantity) {
        buyerPort.addActiveCartLine(buyer, reference(product), quantity);
        return buyerPort.confirmActiveCart(buyer, new Order(null, null, null));
    }

    protected Order paidOrder(Buyer buyer, Product product, int quantity, Administrator administrator) {
        Order order = pendingOrder(buyer, product, quantity);
        return administratorPort.registerOrderPayment(administrator, orderReference(order));
    }

    /** A paid order with a {@code PACKED} shipment. */
    protected Shipment packedShipment(Buyer buyer, Product product, int quantity, Administrator administrator,
                                      LogisticsOperator operator) {
        Order order = paidOrder(buyer, product, quantity, administrator);
        Shipment shipment = logisticsOperatorPort.createShipment(operator,
                new Shipment(unique("SHP"), orderReference(order)));
        return logisticsOperatorPort.packShipment(operator, new Shipment(shipment.getIdentifier(), null));
    }

    /** A delivered order of {@code buyer}. */
    protected Order deliveredOrder(Buyer buyer, Product product, int quantity, Administrator administrator,
                                   LogisticsOperator operator) {
        Shipment shipment = packedShipment(buyer, product, quantity, administrator, operator);
        logisticsOperatorPort.dispatchShipment(operator, new Shipment(shipment.getIdentifier(), null));
        logisticsOperatorPort.confirmDelivery(operator, new Shipment(shipment.getIdentifier(), null));
        return orders.findById(shipment.getOrder().getIdentifier()).orElseThrow();
    }

    /** A {@code REQUESTED} return of a delivered order. */
    protected ReturnRequest requestedReturn(Buyer buyer, Product product, int quantity, Administrator administrator,
                                            LogisticsOperator operator) {
        Order order = deliveredOrder(buyer, product, quantity, administrator, operator);
        return buyerPort.requestReturn(buyer, new ReturnRequest(unique("RET"), orderReference(order), null,
                "Damaged on arrival"));
    }

    /** The {@code PENDING} refund of an approved return. */
    protected Refund pendingRefund(Buyer buyer, Product product, int quantity, Administrator administrator,
                                   LogisticsOperator operator) {
        ReturnRequest request = requestedReturn(buyer, product, quantity, administrator, operator);
        administratorPort.approveReturn(administrator, new ReturnRequest(request.getIdentifier(), null, null, null));
        return refunds.findByReturnRequestId(request.getIdentifier()).orElseThrow();
    }

    protected static Product reference(Product product) {
        return new Product(product.getIdentifier(), null, null, null);
    }

    protected static Order orderReference(Order order) {
        return new Order(order.getIdentifier(), null, null);
    }

    protected ShoppingCart activeCartOf(Buyer buyer) {
        return carts.findByBuyerId(buyer.getIdentifier()).stream().findFirst().orElse(null);
    }

    /** The outcome of one racing call: its result, or the exception it threw. */
    protected record Outcome<T>(T result, RuntimeException failure) {

        boolean succeeded() {
            return failure == null;
        }
    }

    /**
     * Runs {@code task} on {@code threads} threads released at the same instant
     * and returns every outcome, in thread order.
     */
    protected static <T> List<Outcome<T>> race(int threads, IntFunction<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Outcome<T>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return new Outcome<>(task.apply(index), null);
                    } catch (RuntimeException e) {
                        return new Outcome<T>(null, e);
                    }
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();
            List<Outcome<T>> outcomes = new ArrayList<>();
            for (Future<Outcome<T>> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    protected static <T> long successes(List<Outcome<T>> outcomes) {
        return outcomes.stream().filter(Outcome::succeeded).count();
    }
}
