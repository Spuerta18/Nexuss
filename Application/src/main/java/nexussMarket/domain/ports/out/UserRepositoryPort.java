package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.exceptions.ConcurrentUpdateException;
import nexussMarket.domain.exceptions.DuplicateResourceException;
import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.SystemRole;

public interface UserRepositoryPort {

    /**
     * Stores a new user. Fails with {@link DuplicateResourceException} if a user with the same identifier or email
     * already exists; the stored one is never replaced.
     */
    User insert(User user);

    /**
     * Stores the changes of an existing user. Fails with {@link ConcurrentUpdateException} if it was saved by
     * someone else since it was read (its version changed); nothing is stored then. The new version is set on the
     * argument.
     */
    User save(User user);

    Optional<User> findById(String identifier);

    Optional<User> findByEmail(String email);

    boolean existsById(String identifier);

    boolean existsByEmail(String email);

    /** Whether at least one user has {@code role}. */
    boolean existsByRole(SystemRole role);
}
