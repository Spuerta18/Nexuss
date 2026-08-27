package nexussMarket.domain.models;

import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * A read-only operational-monitoring role with no transactional authority. A
 * supervisor may consult operational information but must not create, modify,
 * or delete business data.
 */
public class Supervisor extends User {

    public Supervisor(String identifier, String fullName, String email, UserStatus status) {
        super(identifier, fullName, email, SystemRole.SUPERVISOR, status);
    }
}