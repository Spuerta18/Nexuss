package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.User;
import nexussMarket.domain.valueobjects.SystemRole;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(String identifier);

    Optional<User> findByEmail(String email);

    boolean existsById(String identifier);

    boolean existsByEmail(String email);

    /** Whether at least one user has {@code role}. */
    boolean existsByRole(SystemRole role);
}
