package nexussMarket.domain.models;

import nexussMarket.domain.valueobjects.SystemRole;
import nexussMarket.domain.valueobjects.UserStatus;

/**
 * Represents any person or system identity authorized to interact with
 * NexusMarket. Centralizes the identity and contact information shared by all
 * participants.
 *
 * <p>Each user has exactly one role. This class cannot be instantiated
 * directly.</p>
 */
public abstract class User {

    private String identifier;
    private String fullName;
    private String email;
    private SystemRole role;
    private UserStatus status;
    private String passwordHash;

    protected User(String identifier, String fullName, String email, SystemRole role, UserStatus status) {
        this.identifier = identifier;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    /** Unique identifier of the user (national ID or business tax ID). */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /** Full name of the person or legal name of the business. */
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** Primary email address, unique across the platform. */
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** Business role defining the user's responsibilities within the system. */
    public SystemRole getRole() {
        return role;
    }

    public void setRole(SystemRole role) {
        this.role = role;
    }

    /** Current operational status of the user. */
    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    /**
     * Hashed credential used to authenticate the user. The domain never sees
     * the plain password nor the hashing algorithm (see {@code PasswordHasherPort}).
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
