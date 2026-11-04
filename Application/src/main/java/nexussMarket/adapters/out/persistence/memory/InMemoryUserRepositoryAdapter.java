package nexussMarket.adapters.out.persistence.memory;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.UserMapper;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.EmailAddress;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/** {@link UserRepositoryPort} on the in-memory {@code users} collection (unique email, versioned). */
@Repository
@Profile("memory")
public class InMemoryUserRepositoryAdapter implements UserRepositoryPort {

    private final InMemoryDatabase database;

    public InMemoryUserRepositoryAdapter(InMemoryDatabase database) {
        this.database = database;
    }

    @Override
    public User insert(User user) {
        database.users.insert(UserMapper.toDocument(user),
                "A user with identifier " + user.getIdentifier() + " or email " + user.getEmail() + " already exists");
        return user;
    }

    @Override
    public User save(User user) {
        UserDocument saved = database.users.save(UserMapper.toDocument(user), "User " + user.getIdentifier());
        user.restoreVersion(saved.getVersion());
        return user;
    }

    @Override
    public Optional<User> findById(String identifier) {
        return database.users.findById(identifier).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String normalized = EmailAddress.normalize(email);
        return database.users.find(document -> Objects.equals(document.getEmail(), normalized)).stream()
                .findFirst().map(UserMapper::toDomain);
    }

    @Override
    public boolean existsById(String identifier) {
        return database.users.findById(identifier).isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        String normalized = EmailAddress.normalize(email);
        return database.users.exists(document -> Objects.equals(document.getEmail(), normalized));
    }

    @Override
    public boolean existsByRole(SystemRole role) {
        return database.users.exists(document -> role.getCode().equals(document.getUserType()));
    }

    @Override
    public ResultPage<User> findPage(SystemRole role, PageQuery page) {
        return InMemoryPages.page(database.users.find(document -> role == null
                        || role.getCode().equals(document.getUserType())),
                Comparator.comparing(UserDocument::getId), page,
                documents -> documents.stream().map(UserMapper::toDomain).toList());
    }
}
