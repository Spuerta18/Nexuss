package nexussMarket.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import nexussMarket.domain.exceptions.DomainException;
import nexussMarket.domain.exceptions.EntityNotFoundException;
import nexussMarket.domain.exceptions.OperationNotAllowedException;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.Product;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.ShoppingCart;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.UserFactory;
import nexussMarket.domain.models.UserFactory.UserData;
import nexussMarket.domain.ports.out.AuditLogRepositoryPort;
import nexussMarket.domain.valueobjects.OperationType;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/** {@code PUT /api/me} (own profile) and emptying the active cart, on the real services. */
class ProfileAndCartScenariosTest extends MemoryScenarioSupport {

    @Autowired
    private AuditLogRepositoryPort auditLogs;

    @Test
    void aBuyerChangesNameAndAddressesButNeverEmailRoleOrStatus() {
        Buyer buyer = buyer();

        User updated = buyerPort.updateProfile(buyer, UserFactory.create(SystemRole.BUYER, new UserData(
                buyer.getIdentifier(), "  Ana Renamed ", "hacker@evil.test", UserStatus.BLOCKED, Map.of(
                        UserFactory.PRIMARY_ADDRESS, "Calle 99", UserFactory.ADDITIONAL_ADDRESSES,
                        List.of("Office", "Parents")))));

        Buyer stored = (Buyer) users.findById(buyer.getIdentifier()).orElseThrow();
        assertEquals("Ana Renamed", updated.getFullName());
        assertEquals("Ana Renamed", stored.getFullName());
        assertEquals("Calle 99", stored.getPrimaryAddress());
        assertEquals(List.of("Office", "Parents"), stored.getAdditionalAddresses());
        assertEquals(buyer.getEmail(), stored.getEmail());
        assertEquals(UserStatus.ACTIVE, stored.getStatus());
        assertEquals(SystemRole.BUYER, stored.getRole());
        assertTrue(auditLogs.findAll().stream().anyMatch(entry -> entry.getOperationType() == OperationType.PROFILE_UPDATE
                && buyer.getIdentifier().equals(entry.getAffectedEntityId())
                && "fullName,primaryAddress,additionalAddresses".equals(entry.getDetails().get("changedFields"))));
    }

    @Test
    void staffChangeTheirNameAndNothingIsRecordedWithoutChanges() {
        Seller seller = seller();
        long before = profileUpdates(seller);

        sellerPort.updateProfile(seller, changes(SystemRole.SELLER, seller, seller.getFullName()));
        assertEquals(before, profileUpdates(seller), "an unchanged profile records nothing");

        sellerPort.updateProfile(seller, changes(SystemRole.SELLER, seller, "Sam Renamed"));
        assertEquals("Sam Renamed", users.findById(seller.getIdentifier()).orElseThrow().getFullName());
        assertEquals(before + 1, profileUpdates(seller));
    }

    @Test
    void aBlankNameOrAnInactiveUserIsRejected() {
        Buyer buyer = buyer();
        assertThrows(IllegalArgumentException.class,
                () -> buyerPort.updateProfile(buyer, changes(SystemRole.BUYER, buyer, " ")));

        buyer.setStatus(UserStatus.BLOCKED);
        assertThrows(DomainException.class,
                () -> buyerPort.updateProfile(buyer, changes(SystemRole.BUYER, buyer, "Blocked Buyer")));
        assertEquals(buyer.getFullName(), users.findById(buyer.getIdentifier()).orElseThrow().getFullName());
    }

    @Test
    void clearingTheActiveCartKeepsItEmptyAndIsIdempotent() {
        Seller seller = seller();
        Product product = stockedProduct(seller, warehouseOf(seller), 5);
        Buyer buyer = buyer();
        buyerPort.addActiveCartLine(buyer, reference(product), 2);

        ShoppingCart cleared = buyerPort.clearActiveCart(buyer);
        ShoppingCart again = buyerPort.clearActiveCart(buyer);

        assertTrue(cleared.getLines().isEmpty());
        assertTrue(again.getLines().isEmpty());
        assertTrue(activeCartOf(buyer).getLines().isEmpty());
        assertEquals(1, auditLogs.findAll().stream().filter(entry -> entry.getOperationType() == OperationType.CART_CLEARING
                && cleared.getIdentifier().equals(entry.getAffectedEntityId())).count());
    }

    @Test
    void clearingNeedsACartAndTheBuyerRole() {
        assertThrows(EntityNotFoundException.class, () -> buyerPort.clearActiveCart(buyer()));
        assertThrows(OperationNotAllowedException.class, () -> buyerPort.clearActiveCart(seller()));
    }

    private static User changes(SystemRole role, User user, String fullName) {
        return UserFactory.create(role, new UserData(user.getIdentifier(), fullName, null, null, Map.of()));
    }

    private long profileUpdates(User user) {
        return auditLogs.findAll().stream().filter(entry -> entry.getOperationType() == OperationType.PROFILE_UPDATE
                && user.getIdentifier().equals(entry.getAffectedEntityId())).count();
    }
}
