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
 *
 * <p>{@code identifier}, {@code email} and {@code role} are fixed when the
 * user is created and have no setters: their uniqueness is checked at
 * registration, so changing them later would bypass that check.</p>
 */
public abstract class User {

    private final String identifier;
    private String fullName;
    private final String email;
    private final SystemRole role;
    private UserStatus status;
    private String passwordHash;
    private int authTokenVersion;

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

    /** Business role defining the user's responsibilities within the system. */
    public SystemRole getRole() {
        return role;
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

    /**
     * Version of the user's access tokens. Every token carries the version it
     * was issued with, and only tokens of the current version are accepted, so
     * raising it revokes every token issued before (logout, password change).
     */
    public int getAuthTokenVersion() {
        return authTokenVersion;
    }

    /** Revokes every access token issued so far. */
    public void bumpAuthTokenVersion() {
        authTokenVersion++;
    }

    /** Restores the stored version when the user is rebuilt from persistence; never lowers it below zero. */
    public void restoreAuthTokenVersion(int authTokenVersion) {
        if (authTokenVersion < 0) {
            throw new IllegalArgumentException("authTokenVersion must never be negative");
        }
        this.authTokenVersion = authTokenVersion;
    }
}
