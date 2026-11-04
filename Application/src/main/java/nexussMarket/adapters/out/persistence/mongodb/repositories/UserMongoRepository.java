package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.UserDocument;

/** Spring Data repository for the {@code users} collection. */
public interface UserMongoRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserType(String userType);

    Page<UserDocument> findByUserType(String userType, Pageable pageable);
}
