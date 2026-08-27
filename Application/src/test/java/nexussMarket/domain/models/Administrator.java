package nexussMarket.domain.models;

import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * A user responsible for administering sellers and Marketplace-owned
 * warehouses. Registers {@link Seller} instances and Marketplace-owned
 * {@link Warehouse} instances.
 */
public class Administrator extends User {

    public Administrator(String identifier, String fullName, String email, UserStatus status) {
        super(identifier, fullName, email, SystemRole.ADMINISTRATOR, status);
    }
}