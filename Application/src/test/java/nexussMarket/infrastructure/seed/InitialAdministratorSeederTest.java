package nexussMarket.infrastructure.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;
import nexussMarket.infrastructure.security.BCryptPasswordHasherAdapter;

class InitialAdministratorSeederTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "s3cret-Pass";

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final BCryptPasswordHasherAdapter hasher = new BCryptPasswordHasherAdapter();

    @Test
    void createsTheAdministratorWithABcryptHash() {
        run(EMAIL, PASSWORD);

        assertEquals(1, users.byId.size());
        Administrator admin = assertInstanceOf(Administrator.class, users.byId.get(InitialAdministratorSeeder.IDENTIFIER));
        assertEquals(EMAIL, admin.getEmail());
        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        assertNotEquals(PASSWORD, admin.getPasswordHash());
        assertTrue(admin.getPasswordHash().startsWith("$2"), "expected a BCrypt hash");
        assertTrue(hasher.matches(PASSWORD, admin.getPasswordHash()));
    }

    @Test
    void isIdempotent() {
        run(EMAIL, PASSWORD);
        String firstHash = users.byId.get(InitialAdministratorSeeder.IDENTIFIER).getPasswordHash();

        run(EMAIL, PASSWORD);

        assertEquals(1, users.byId.size());
        assertEquals(1, users.saves);
        assertEquals(firstHash, users.byId.get(InitialAdministratorSeeder.IDENTIFIER).getPasswordHash());
    }

    @Test
    void createsNothingWithoutTheVariablesAndDoesNotQueryTheRepository() {
        run(null, null);
        run(EMAIL, "");
        run(" ", PASSWORD);

        assertTrue(users.byId.isEmpty());
        assertEquals(0, users.queries);
    }

    @Test
    void createsNothingWhenAnAdministratorAlreadyExists() {
        users.save(new Administrator("A-1", "Existing Admin", "other@example.com", UserStatus.ACTIVE));

        run(EMAIL, PASSWORD);

        assertEquals(1, users.byId.size());
        assertEquals(1, users.saves);
    }

    @Test
    void createsNothingWhenTheEmailBelongsToAnotherUser() {
        users.save(new Buyer("B-1", "Ana Smith", EMAIL, UserStatus.ACTIVE, CustomerStatus.ENABLED));

        run(EMAIL, PASSWORD);

        assertEquals(1, users.byId.size());
        assertInstanceOf(Buyer.class, users.byId.get("B-1"));
    }

    private void run(String email, String password) {
        new InitialAdministratorSeeder(users, hasher, email, password).run(new DefaultApplicationArguments());
    }

    /** Minimal in-memory {@link UserRepositoryPort}, counting saves and queries. */
    private static final class InMemoryUserRepository implements UserRepositoryPort {

        private final Map<String, User> byId = new HashMap<>();
        private int saves;
        private int queries;

        @Override
        public User save(User user) {
            saves++;
            byId.put(user.getIdentifier(), user);
            return user;
        }

        @Override
        public Optional<User> findById(String identifier) {
            queries++;
            return Optional.ofNullable(byId.get(identifier));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            queries++;
            return byId.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        @Override
        public boolean existsById(String identifier) {
            queries++;
            return byId.containsKey(identifier);
        }

        @Override
        public boolean existsByEmail(String email) {
            queries++;
            return byId.values().stream().anyMatch(u -> u.getEmail().equals(email));
        }

        @Override
        public boolean existsByRole(SystemRole role) {
            queries++;
            return byId.values().stream().anyMatch(u -> u.getRole() == role);
        }
    }
}
