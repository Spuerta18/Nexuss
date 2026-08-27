package nexussMarket.domain.valueobjects;

import java.util.List;

/**
 * Operational status of a {@link nexussMarket.domain.models.User} with respect
 * to system access.
 */
public final class UserStatus extends DomainCatalog {

    public static final UserStatus ACTIVE =
            new UserStatus("ACTIVE", "Active", "The user may access and operate the system.");
    public static final UserStatus BLOCKED =
            new UserStatus("BLOCKED", "Blocked", "The user's access has been suspended.");
    public static final UserStatus INACTIVE =
            new UserStatus("INACTIVE", "Inactive", "The user is registered but not currently operational.");

    private UserStatus(String code, String name, String description) {
        super(code, name, description);
    }

    /** Returns the full {@code UserStatus} value list. */
    public static List<UserStatus> values() {
        return List.of(ACTIVE, BLOCKED, INACTIVE);
    }

    /** Resolves a {@code UserStatus} from its code, or {@code null} if unknown. */
    public static UserStatus fromCode(String code) {
        return values().stream()
                .filter(s -> s.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}