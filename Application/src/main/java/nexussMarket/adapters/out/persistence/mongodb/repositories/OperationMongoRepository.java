package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.OperationDocument;

/**
 * Spring Data repository for the append-only {@code operations} collection.
 * It extends the bare {@link Repository} and declares only {@code insert} and
 * reads, so no update or delete method exists. {@code insert} is served by
 * the Spring Data base implementation and fails on an existing identifier.
 */
public interface OperationMongoRepository extends Repository<OperationDocument, String> {

    <S extends OperationDocument> S insert(S document);

    List<OperationDocument> findAll(Sort sort);
}
