package nexussMarket.infrastructure.seed;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.InventoryItem;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.Warehouse;
import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;
import nexussMarket.domain.valueobjects.Currency;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.Money;
import nexussMarket.domain.valueobjects.ProductStatus;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.domain.valueobjects.WarehouseOwnerType;

/**
 * Creates demonstration data at startup when {@code nexus.seed.demo.enabled}
 * is {@code true} (the default of the {@code memory} profile; off otherwise):
 * one account per role, a warehouse of the demo seller, three published
 * physical products with stock and one digital product.
 *
 * <p>Idempotent: every record has a fixed identifier and is only inserted when
 * missing, so restarting against MongoDB changes nothing. All accounts share
 * one password, taken from {@code NEXUS_DEMO_PASSWORD}; without it a random
 * password is generated and written to the log once, only when the accounts
 * are created. Passwords are never stored in the repository.</p>
 */
public class DemoDataSeeder implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "admin@demo.nexus";
    public static final String SELLER_EMAIL = "seller@demo.nexus";
    public static final String BUYER_EMAIL = "buyer@demo.nexus";
    public static final String LOGISTICS_EMAIL = "logistics@demo.nexus";
    public static final String SUPERVISOR_EMAIL = "supervisor@demo.nexus";

    public static final String SELLER_ID = "DEMO-SELLER";
    public static final String WAREHOUSE_ID = "WH-DEMO-1";
    public static final List<String> PHYSICAL_PRODUCT_IDS = List.of("PRD-DEMO-1", "PRD-DEMO-2", "PRD-DEMO-3");
    public static final String DIGITAL_PRODUCT_ID = "PRD-DEMO-4";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepositoryPort userRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final String configuredPassword;

    public DemoDataSeeder(UserRepositoryPort userRepositoryPort, WarehouseRepositoryPort warehouseRepositoryPort,
                          ProductRepositoryPort productRepositoryPort, InventoryRepositoryPort inventoryRepositoryPort,
                          PasswordHasherPort passwordHasherPort, String configuredPassword) {
        this.userRepositoryPort = userRepositoryPort;
        this.warehouseRepositoryPort = warehouseRepositoryPort;
        this.productRepositoryPort = productRepositoryPort;
        this.inventoryRepositoryPort = inventoryRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean generated = configuredPassword == null || configuredPassword.isBlank();
        String password = generated ? randomPassword() : configuredPassword;
        String hash = passwordHasherPort.hash(password);

        Buyer buyer = new Buyer("DEMO-BUYER", "Demo Buyer", BUYER_EMAIL, UserStatus.ACTIVE, CustomerStatus.ENABLED);
        buyer.setPrimaryAddress("Calle 10 # 20-30, Bogota");
        Seller seller = new Seller(SELLER_ID, "Demo Seller", SELLER_EMAIL, UserStatus.ACTIVE, SellerStatus.ACTIVE);
        int created = 0;
        created += insertUser(new Administrator("DEMO-ADMIN", "Demo Administrator", ADMIN_EMAIL, UserStatus.ACTIVE),
                hash);
        created += insertUser(seller, hash);
        created += insertUser(buyer, hash);
        created += insertUser(new LogisticsOperator("DEMO-LOGISTICS", "Demo Logistics Operator", LOGISTICS_EMAIL,
                UserStatus.ACTIVE), hash);
        created += insertUser(new Supervisor("DEMO-SUPERVISOR", "Demo Supervisor", SUPERVISOR_EMAIL,
                UserStatus.ACTIVE), hash);

        Seller storedSeller = (Seller) userRepositoryPort.findById(SELLER_ID).orElseThrow();
        if (warehouseRepositoryPort.findById(WAREHOUSE_ID).isEmpty()) {
            insert(() -> warehouseRepositoryPort.insert(new Warehouse(WAREHOUSE_ID, "Demo Seller Warehouse",
                    "Carrera 7 # 45-10, Bogota", WarehouseOwnerType.SELLER, storedSeller, true)));
        }
        Warehouse warehouse = warehouseRepositoryPort.findById(WAREHOUSE_ID).orElseThrow();
        physicalProduct(PHYSICAL_PRODUCT_IDS.get(0), "Mechanical Keyboard", 180000, 50, storedSeller, warehouse);
        physicalProduct(PHYSICAL_PRODUCT_IDS.get(1), "Wireless Mouse", 45000, 30, storedSeller, warehouse);
        physicalProduct(PHYSICAL_PRODUCT_IDS.get(2), "27-inch Monitor", 950000, 10, storedSeller, warehouse);
        if (productRepositoryPort.findById(DIGITAL_PRODUCT_ID).isEmpty()) {
            insert(() -> productRepositoryPort.insert(new Product(DIGITAL_PRODUCT_ID, "Java Course (e-book)",
                    ProductType.DIGITAL, ProductStatus.PUBLISHED, storedSeller, cop(60000), List.of())));
        }

        if (created == 0) {
            log.info("Demo data already present; demo accounts keep their existing password");
        } else if (generated) {
            log.warn("Demo accounts created ({}, {}, {}, {}, {}) with the generated password: {}"
                            + "  (set NEXUS_DEMO_PASSWORD to choose it; this is the only time it is shown)",
                    ADMIN_EMAIL, SELLER_EMAIL, BUYER_EMAIL, LOGISTICS_EMAIL, SUPERVISOR_EMAIL, password);
        } else {
            log.info("Demo accounts created ({}, {}, {}, {}, {}) with the password from NEXUS_DEMO_PASSWORD",
                    ADMIN_EMAIL, SELLER_EMAIL, BUYER_EMAIL, LOGISTICS_EMAIL, SUPERVISOR_EMAIL);
        }
    }

    private int insertUser(User user, String hash) {
        if (userRepositoryPort.existsById(user.getIdentifier()) || userRepositoryPort.existsByEmail(user.getEmail())) {
            return 0;
        }
        user.setPasswordHash(hash);
        try {
            userRepositoryPort.insert(user);
            return 1;
        } catch (DuplicateResourceException e) {
            return 0;
        }
    }

    private void physicalProduct(String id, String name, long price, int stock, Seller seller, Warehouse warehouse) {
        Product product = productRepositoryPort.findById(id).orElseGet(() -> insert(() -> productRepositoryPort
                .insert(new Product(id, name, ProductType.PHYSICAL, ProductStatus.PUBLISHED, seller, cop(price),
                        List.of()))));
        if (product == null) {
            product = productRepositoryPort.findById(id).orElseThrow();
        }
        if (inventoryRepositoryPort.findByProductIdAndWarehouseId(id, warehouse.getIdentifier()).isEmpty()) {
            Product stocked = product;
            insert(() -> inventoryRepositoryPort.insert(new InventoryItem(stocked, warehouse, stock, 0, 0)));
        }
    }

    /** Inserts, tolerating another instance that inserted the same record first. */
    private static <T> T insert(Supplier<T> insert) {
        try {
            return insert.get();
        } catch (DuplicateResourceException e) {
            return null;
        }
    }

    private static Money cop(long amount) {
        return Money.of(BigDecimal.valueOf(amount), Currency.COP);
    }

    private static String randomPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return "Demo-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
