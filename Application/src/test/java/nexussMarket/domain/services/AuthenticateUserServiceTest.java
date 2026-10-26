package nexussMarket.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import nexussMarket.domain.exceptions.InvalidCredentialsException;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.AuthenticationResult;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.PasswordHasherPort;
import nexussMarket.domain.ports.out.TokenServicePort;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.AffectedEntityType;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

class AuthenticateUserServiceTest extends AuditedServiceTestSupport {

    private static final String PASSWORD = "Correct-Pass-1";

    private final InMemoryUsers users = new InMemoryUsers();
    private final CountingTokens tokens = new CountingTokens();
    private final AuthenticateUserService service = new AuthenticateUserService(users, new PlainHasher(), tokens,
            audit);

    @Test
    void activeUserWithTheRightPasswordGetsAToken() {
        Buyer buyer = buyer(UserStatus.ACTIVE);
        users.save(buyer);

        AuthenticationResult result = service.execute("ana@example.com", PASSWORD);

        assertSame(buyer, result.getUser());
        assertEquals("token-for-B-1", result.getToken());
        AuditLog entry = assertAudited(OperationType.USER_LOGIN, buyer, AffectedEntityType.USER, "B-1");
        assertFalse(entry.getDetails().containsValue("token-for-B-1"), "the token is never audited");
    }

    @Test
    void everyFailedLoginFailsTheSameWay() {
        users.save(buyer(UserStatus.ACTIVE));
        users.save(new Buyer("B-2", "Blocked Buyer", "blocked@example.com", UserStatus.BLOCKED, CustomerStatus.ENABLED));
        users.save(new Buyer("B-3", "Gone Buyer", "inactive@example.com", UserStatus.INACTIVE, CustomerStatus.ENABLED));
        users.byId.get("B-2").setPasswordHash("hash:" + PASSWORD);
        users.byId.get("B-3").setPasswordHash("hash:" + PASSWORD);

        String unknownEmail = failure("nobody@example.com", PASSWORD);
        String wrongPassword = failure("ana@example.com", "Wrong-Pass-1");
        String blocked = failure("blocked@example.com", PASSWORD);
        String inactive = failure("inactive@example.com", PASSWORD);

        assertEquals("Invalid email or password", unknownEmail);
        assertEquals(unknownEmail, wrongPassword);
        assertEquals(unknownEmail, blocked);
        assertEquals(unknownEmail, inactive);
        assertEquals(0, tokens.issued, "no token may be issued for a failed login");
        assertNothingAudited();
    }

    private String failure(String email, String password) {
        return assertThrows(InvalidCredentialsException.class, () -> service.execute(email, password)).getMessage();
    }

    private static Buyer buyer(UserStatus status) {
        Buyer buyer = new Buyer("B-1", "Ana Buyer", "ana@example.com", status, CustomerStatus.ENABLED);
        buyer.setPasswordHash("hash:" + PASSWORD);
        return buyer;
    }

    /** Deterministic stand-in for BCrypt: the hash is {@code "hash:" + password}. */
    private static final class PlainHasher implements PasswordHasherPort {

        @Override
        public String hash(String rawPassword) {
            return "hash:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals("hash:" + rawPassword);
        }
    }

    private static final class CountingTokens implements TokenServicePort {

        private int issued;

        @Override
        public String generateToken(User user) {
            issued++;
            return "token-for-" + user.getIdentifier();
        }
    }

    private static final class InMemoryUsers implements UserRepositoryPort {

        private final Map<String, User> byId = new HashMap<>();

        @Override
        public User save(User user) {
            byId.put(user.getIdentifier(), user);
            return user;
        }

        @Override
        public Optional<User> findById(String identifier) {
            return Optional.ofNullable(byId.get(identifier));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return byId.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }

        @Override
        public boolean existsById(String identifier) {
            return byId.containsKey(identifier);
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public boolean existsByRole(SystemRole role) {
            return byId.values().stream().anyMatch(u -> u.getRole() == role);
        }
    }
}
