package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.util.ArrayList;

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

/**
 * Converts every {@link User} specialization to and from {@link UserDocument},
 * using {@code userType} (the {@link SystemRole} code) as the discriminator.
 * Inverse lists ({@code Buyer.carts} / {@code orders}, {@code Seller.warehouses}
 * / {@code products}) are not stored and are rebuilt empty.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserDocument toDocument(User user) {
        UserDocument document = new UserDocument();
        document.setId(user.getIdentifier());
        document.setUserType(user.getRole().getCode());
        document.setFullName(user.getFullName());
        document.setEmail(user.getEmail());
        document.setStatus(user.getStatus().getCode());
        document.setPasswordHash(user.getPasswordHash());
        if (user instanceof Buyer buyer) {
            document.setPrimaryAddress(buyer.getPrimaryAddress());
            document.setAdditionalAddresses(new ArrayList<>(buyer.getAdditionalAddresses()));
            document.setCommercialStatus(buyer.getCommercialStatus().getCode());
        } else if (user instanceof Seller seller) {
            document.setSellerStatus(seller.getSellerStatus().getCode());
        }
        return document;
    }

    public static User toDomain(UserDocument document) {
        User user = newUser(document);
        user.setPasswordHash(document.getPasswordHash());
        return user;
    }

    private static User newUser(UserDocument document) {
        String id = document.getId();
        String fullName = document.getFullName();
        String email = document.getEmail();
        UserStatus status = CatalogCodes.resolve(UserStatus::fromCode, document.getStatus(), "UserStatus");
        SystemRole role = SystemRole.fromCode(document.getUserType());
        if (role == SystemRole.BUYER) {
            Buyer buyer = new Buyer(id, fullName, email, status,
                    CatalogCodes.resolve(CustomerStatus::fromCode, document.getCommercialStatus(), "CustomerStatus"));
            buyer.setPrimaryAddress(document.getPrimaryAddress());
            buyer.setAdditionalAddresses(document.getAdditionalAddresses());
            return buyer;
        }
        if (role == SystemRole.SELLER) {
            return new Seller(id, fullName, email, status,
                    CatalogCodes.resolve(SellerStatus::fromCode, document.getSellerStatus(), "SellerStatus"));
        }
        if (role == SystemRole.LOGISTICS_OPERATOR) {
            return new LogisticsOperator(id, fullName, email, status);
        }
        if (role == SystemRole.ADMINISTRATOR) {
            return new Administrator(id, fullName, email, status);
        }
        if (role == SystemRole.SUPERVISOR) {
            return new Supervisor(id, fullName, email, status);
        }
        throw new IllegalStateException("User " + id + " has unknown userType " + document.getUserType());
    }
}
