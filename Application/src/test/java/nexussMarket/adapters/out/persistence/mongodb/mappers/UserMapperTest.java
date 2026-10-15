package nexussMarket.adapters.out.persistence.mongodb.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.domain.models.Administrator;
import nexussMarket.domain.models.Buyer;
import nexussMarket.domain.models.LogisticsOperator;
import nexussMarket.domain.models.Seller;
import nexussMarket.domain.models.Supervisor;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.CustomerStatus;
import nexussMarket.domain.valueobjects.SellerStatus;
import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

class UserMapperTest {

    @Test
    void rebuildsBuyerFromItsUserType() {
        UserDocument document = baseDocument("BUYER");
        document.setCommercialStatus("ENABLED");

        User user = UserMapper.toDomain(document);

        Buyer buyer = assertInstanceOf(Buyer.class, user);
        assertEquals(SystemRole.BUYER, buyer.getRole());
        assertEquals(CustomerStatus.ENABLED, buyer.getCommercialStatus());
    }

    @Test
    void rebuildsSellerFromItsUserType() {
        UserDocument document = baseDocument("SELLER");
        document.setSellerStatus("ACTIVE");

        User user = UserMapper.toDomain(document);

        Seller seller = assertInstanceOf(Seller.class, user);
        assertEquals(SystemRole.SELLER, seller.getRole());
        assertEquals(SellerStatus.ACTIVE, seller.getSellerStatus());
    }

    @Test
    void rebuildsStaffRolesFromTheirUserType() {
        assertRebuiltAs("LOGISTICS_OPERATOR", LogisticsOperator.class, SystemRole.LOGISTICS_OPERATOR);
        assertRebuiltAs("ADMINISTRATOR", Administrator.class, SystemRole.ADMINISTRATOR);
        assertRebuiltAs("SUPERVISOR", Supervisor.class, SystemRole.SUPERVISOR);
    }

    @Test
    void buyerSurvivesRoundTrip() {
        Buyer buyer = new Buyer("1020304050", "Ana Smith", "ana@example.com", UserStatus.ACTIVE,
                CustomerStatus.ENABLED);
        buyer.setPrimaryAddress("1 Main Street");
        buyer.setAdditionalAddresses(List.of("2 Side Street"));
        buyer.setPasswordHash("hash");

        Buyer rebuilt = assertInstanceOf(Buyer.class, UserMapper.toDomain(UserMapper.toDocument(buyer)));

        assertEquals(buyer.getIdentifier(), rebuilt.getIdentifier());
        assertEquals(buyer.getEmail(), rebuilt.getEmail());
        assertEquals(UserStatus.ACTIVE, rebuilt.getStatus());
        assertEquals("1 Main Street", rebuilt.getPrimaryAddress());
        assertEquals(List.of("2 Side Street"), rebuilt.getAdditionalAddresses());
        assertEquals("hash", rebuilt.getPasswordHash());
    }

    @Test
    void unknownUserTypeFails() {
        assertThrows(IllegalStateException.class, () -> UserMapper.toDomain(baseDocument("GUEST")));
    }

    private static void assertRebuiltAs(String userType, Class<? extends User> type, SystemRole role) {
        User user = UserMapper.toDomain(baseDocument(userType));
        assertInstanceOf(type, user);
        assertEquals(role, user.getRole());
    }

    private static UserDocument baseDocument(String userType) {
        UserDocument document = new UserDocument();
        document.setId("U-1");
        document.setUserType(userType);
        document.setFullName("Test User");
        document.setEmail("user@example.com");
        document.setStatus("ACTIVE");
        return document;
    }
}
