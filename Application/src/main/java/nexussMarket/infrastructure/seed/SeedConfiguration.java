package nexussMarket.infrastructure.seed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.out.InventoryRepositoryPort;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.ProductRepositoryPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.ports.out.WarehouseRepositoryPort;

/**
 * Registers the startup seeders. The initial administrator's credentials are
 * read from {@code nexus.seed.admin.*}, which {@code application.properties}
 * binds to the {@code NEXUS_ADMIN_EMAIL} and {@code NEXUS_ADMIN_PASSWORD}
 * environment variables.
 */
@Configuration
public class SeedConfiguration {

    @Bean
    public InitialAdministratorSeeder initialAdministratorSeeder(UserRepositoryPort userRepositoryPort,
                                                                 PasswordHasherPort passwordHasherPort,
                                                                 @Value("${nexus.seed.admin.email:}") String email,
                                                                 @Value("${nexus.seed.admin.password:}") String password) {
        return new InitialAdministratorSeeder(userRepositoryPort, passwordHasherPort, email, password);
    }

    /** Demo data; only when {@code nexus.seed.demo.enabled=true} (default of the {@code memory} profile). */
    @Bean
    @ConditionalOnProperty(name = "nexus.seed.demo.enabled", havingValue = "true")
    public DemoDataSeeder demoDataSeeder(UserRepositoryPort userRepositoryPort,
                                         WarehouseRepositoryPort warehouseRepositoryPort,
                                         ProductRepositoryPort productRepositoryPort,
                                         InventoryRepositoryPort inventoryRepositoryPort,
                                         PasswordHasherPort passwordHasherPort,
                                         @Value("${nexus.seed.demo.password:}") String password) {
        return new DemoDataSeeder(userRepositoryPort, warehouseRepositoryPort, productRepositoryPort,
                inventoryRepositoryPort, passwordHasherPort, password);
    }
}
