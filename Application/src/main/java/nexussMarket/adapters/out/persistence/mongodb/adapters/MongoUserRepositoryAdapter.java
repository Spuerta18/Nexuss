package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.UserMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.UserMongoRepository;
import nexussMarket.domain.models.ResultPage;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.UserRepositoryPort;
import nexussMarket.domain.valueobjects.EmailAddress;
import nexussMarket.domain.valueobjects.PageQuery;
import nexussMarket.domain.valueobjects.SystemRole;

/** Implements {@link UserRepositoryPort} on the {@code users} collection. */
@Repository
@Profile("!memory")
public class MongoUserRepositoryAdapter implements UserRepositoryPort {

    private final UserMongoRepository userRepository;

    public MongoUserRepositoryAdapter(UserMongoRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** A real {@code insert}: an existing identifier or email (unique index) is never replaced. */
    @Override
    public User insert(User user) {
        MongoInsertion.insert(() -> userRepository.insert(UserMapper.toDocument(user)),
                "A user with identifier " + user.getIdentifier() + " or email " + user.getEmail() + " already exists");
        return user;
    }

    @Override
    public User save(User user) {
        user.restoreVersion(MongoVersioning.save(() -> userRepository.save(UserMapper.toDocument(user)),
                "User " + user.getIdentifier()).getVersion());
        return user;
    }

    @Override
    public Optional<User> findById(String identifier) {
        return userRepository.findById(identifier).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(EmailAddress.normalize(email)).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsById(String identifier) {
        return userRepository.existsById(identifier);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(EmailAddress.normalize(email));
    }

    @Override
    public boolean existsByRole(SystemRole role) {
        return userRepository.existsByUserType(role.getCode());
    }

    @Override
    public ResultPage<User> findPage(SystemRole role, PageQuery page) {
        Pageable pageable = MongoPages.pageable(page, Sort.by("_id"));
        Page<UserDocument> documents = role != null
                ? userRepository.findByUserType(role.getCode(), pageable)
                : userRepository.findAll(pageable);
        return MongoPages.toResultPage(documents, page,
                content -> content.stream().map(UserMapper::toDomain).toList());
    }
}
