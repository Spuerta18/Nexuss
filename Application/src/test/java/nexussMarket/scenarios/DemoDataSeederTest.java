package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.valueobjects.ProductType;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.infrastructure.seed.DemoDataSeeder;

/** The demo data of the {@code memory} profile: one account per role and the catalog, and running it again changes nothing. */
class DemoDataSeederTest extends MemoryScenarioSupport {

    @Autowired
    private DemoDataSeeder seeder;
    @Autowired
    private ProductRepositoryPort products;

    @Test
    void demoDataHasOneAccountPerRoleAndAStockedCatalog() {
        for (SystemRole role : SystemRole.values()) {
            assertTrue(users.existsByRole(role), "a demo account for " + role.getCode());
        }
        for (String id : DemoDataSeeder.PHYSICAL_PRODUCT_IDS) {
            assertEquals(ProductType.PHYSICAL, products.findById(id).orElseThrow().getProductType());
            assertTrue(inventory.findByProductIdAndWarehouseId(id, DemoDataSeeder.WAREHOUSE_ID).isPresent());
        }
        assertEquals(ProductType.DIGITAL,
                products.findById(DemoDataSeeder.DIGITAL_PRODUCT_ID).orElseThrow().getProductType());
    }

    @Test
    void runningTheSeederAgainChangesNothing() throws Exception {
        User admin = users.findByEmail(DemoDataSeeder.ADMIN_EMAIL).orElseThrow();
        String id = DemoDataSeeder.PHYSICAL_PRODUCT_IDS.get(2);
        int stock = inventory.findByProductIdAndWarehouseId(id, DemoDataSeeder.WAREHOUSE_ID).orElseThrow()
                .getAvailableQuantity();

        seeder.run(null);

        User again = users.findByEmail(DemoDataSeeder.ADMIN_EMAIL).orElseThrow();
        assertEquals(admin.getPasswordHash(), again.getPasswordHash(), "the password is not replaced");
        assertEquals(admin.getVersion(), again.getVersion());
        assertEquals(stock, inventory.findByProductIdAndWarehouseId(id, DemoDataSeeder.WAREHOUSE_ID).orElseThrow()
                .getAvailableQuantity());
    }
}
