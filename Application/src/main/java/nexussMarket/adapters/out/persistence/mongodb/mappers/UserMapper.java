package nexussMarket.adapters.out.persistence.mongodb.mappers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.domain.models.User;
import nexussMarket.domain.models.UserFactory;
import nexussMarket.domain.models.UserFactory.UserData;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Converts every {@link User} specialization to and from {@link UserDocument},
 * using {@code userType} (the {@code SystemRole} code) as the discriminator.
 * Users are built through {@link UserFactory}, so a newly registered user type
 * needs no change here: its type-specific attributes are stored in the
 * buyer / seller fields when they have one, and in {@code attributes}
 * otherwise. Inverse lists ({@code Buyer.carts} / {@code orders},
 * {@code Seller.warehouses} / {@code products}) are not stored and are rebuilt
 * empty.
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
        document.setAuthTokenVersion(user.getAuthTokenVersion());
        document.setVersion(user.getVersion());
        Map<String, Object> others = new LinkedHashMap<>();
        UserFactory.attributesOf(user).forEach((name, value) -> {
            switch (name) {
                case UserFactory.PRIMARY_ADDRESS -> document.setPrimaryAddress((String) value);
                case UserFactory.ADDITIONAL_ADDRESSES -> document.setAdditionalAddresses(stringList(value));
                case UserFactory.COMMERCIAL_STATUS -> document.setCommercialStatus((String) value);
                case UserFactory.SELLER_STATUS -> document.setSellerStatus((String) value);
                default -> others.put(name, value);
            }
        });
        document.setAttributes(others.isEmpty() ? null : others);
        return document;
    }

    public static User toDomain(UserDocument document) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (document.getAttributes() != null) {
            attributes.putAll(document.getAttributes());
        }
        attributes.put(UserFactory.PRIMARY_ADDRESS, document.getPrimaryAddress());
        attributes.put(UserFactory.ADDITIONAL_ADDRESSES, document.getAdditionalAddresses());
        attributes.put(UserFactory.COMMERCIAL_STATUS, document.getCommercialStatus());
        attributes.put(UserFactory.SELLER_STATUS, document.getSellerStatus());
        UserStatus status = CatalogCodes.resolve(UserStatus::fromCode, document.getStatus(), "UserStatus");
        User user = UserFactory.create(document.getUserType(), new UserData(document.getId(),
                document.getFullName(), document.getEmail(), status, attributes));
        user.setPasswordHash(document.getPasswordHash());
        user.restoreAuthTokenVersion(document.getAuthTokenVersion() != null ? document.getAuthTokenVersion() : 0);
        user.restoreVersion(VersionedDocuments.versionOf(document.getVersion()));
        return user;
    }

    private static List<String> stringList(Object value) {
        List<String> list = new ArrayList<>();
        if (value instanceof List<?> values) {
            values.forEach(element -> list.add(String.valueOf(element)));
        }
        return list;
    }
}
