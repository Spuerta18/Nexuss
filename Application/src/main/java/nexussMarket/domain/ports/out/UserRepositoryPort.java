package nexussMarket.domain.ports.out;

import java.util.Optional;

import nexussMarket.domain.models.User;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(String identifier);

    Optional<User> findByEmail(String email);

    boolean existsById(String identifier);

    boolean existsByEmail(String email);
}
