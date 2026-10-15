package nexussMarket.infrastructure.seed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.UserRepositoryPort;

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
}
