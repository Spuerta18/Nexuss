package nexussMarket.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import nexussMarket.adapters.in.rest.mappers.DomainReferences;
import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.UserMapper;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * {@link UserFactory} is the only place that knows the user types: a new one,
 * registered here in the test, goes through the persistence mapper and the
 * REST references without any change to them.
 */
class UserFactoryTest {

    /** A user type the application does not have: an external auditor with a badge number. */
    static final class Auditor extends User {

        static final SystemRole ROLE = SystemRole.extension("AUDITOR", "Auditor", "External, read-only auditor.");

        private final String badge;

        Auditor(String identifier, String fullName, String email, UserStatus status, String badge) {
            super(identifier, fullName, email, ROLE, status);
            this.badge = badge;
        }

        String getBadge() {
            return badge;
        }
    }

    static {
        UserFactory.register(new UserFactory.UserType<>(Auditor.ROLE, Auditor.class,
                data -> new Auditor(data.identifier(), data.fullName(), data.email(), data.status(),
                        data.attribute("badge")),
                auditor -> auditor.getBadge() != null ? Map.of("badge", auditor.getBadge()) : Map.of()));
    }

    @Test
    void aNewUserTypeRoundTripsThroughTheUnchangedPersistenceMapper() {
        Auditor auditor = new Auditor("AUD-1", "Ada Auditor", "Ada@Example.com", UserStatus.ACTIVE, "B-77");
        auditor.restoreVersion(3);

        UserDocument document = UserMapper.toDocument(auditor);
        User rebuilt = UserMapper.toDomain(document);

        assertEquals("AUDITOR", document.getUserType());
        assertEquals(Map.of("badge", "B-77"), document.getAttributes());
        Auditor copy = assertInstanceOf(Auditor.class, rebuilt);
        assertEquals("B-77", copy.getBadge());
        assertEquals("ada@example.com", copy.getEmail());
        assertEquals(3, copy.getVersion());
    }

    @Test
    void aNewUserTypeCanBeReferencedFromTheRestLayer() {
        User reference = DomainReferences.user("AUD-2", UserFactory.role("AUDITOR"));

        assertInstanceOf(Auditor.class, reference);
        assertEquals("AUD-2", reference.getIdentifier());
    }

    @Test
    void theStandardTypesKeepTheirAttributes() {
        Buyer buyer = (Buyer) UserFactory.create(SystemRole.BUYER, new UserFactory.UserData("B-1", "Ana", "ana@x.com",
                UserStatus.ACTIVE, Map.of(UserFactory.PRIMARY_ADDRESS, "1 Main", UserFactory.COMMERCIAL_STATUS,
                "ENABLED")));

        assertEquals("1 Main", buyer.getPrimaryAddress());
        assertEquals("ENABLED", UserFactory.attributesOf(buyer).get(UserFactory.COMMERCIAL_STATUS));
    }

    @Test
    void anUnknownRoleFailsNamingTheRoleAndWhereToRegisterIt() {
        UserDocument document = new UserDocument();
        document.setId("X-1");
        document.setUserType("ROBOT");
        document.setStatus("ACTIVE");

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> UserMapper.toDomain(document));

        assertTrue(failure.getMessage().contains("'ROBOT'"), failure.getMessage());
        assertTrue(failure.getMessage().contains("UserFactory.register"), failure.getMessage());
    }
}
