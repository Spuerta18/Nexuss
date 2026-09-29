package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.UserMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.UserMongoRepository;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;

/** Implements {@link UserRepositoryPort} on the {@code users} collection. */
@Repository
public class MongoUserRepositoryAdapter implements UserRepositoryPort {

    private final UserMongoRepository userRepository;

    public MongoUserRepositoryAdapter(UserMongoRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User save(User user) {
        userRepository.save(UserMapper.toDocument(user));
        return user;
    }

    @Override
    public Optional<User> findById(String identifier) {
        return userRepository.findById(identifier).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsById(String identifier) {
        return userRepository.existsById(identifier);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
