package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.OperationDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.OperationMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.OperationMongoRepository;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OperationRepositoryPort;

/**
 * Implements {@link OperationRepositoryPort} on the append-only
 * {@code operations} collection: entries are only ever inserted. Reads resolve
 * every performer with a single query.
 */
@Repository
public class MongoOperationRepositoryAdapter implements OperationRepositoryPort {

    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "executionDate");

    private final OperationMongoRepository operationRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoOperationRepositoryAdapter(OperationMongoRepository operationRepository,
                                           MongoReferenceResolver referenceResolver) {
        this.operationRepository = operationRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Operation save(Operation operation) {
        operationRepository.insert(OperationMapper.toDocument(operation));
        return operation;
    }

    @Override
    public List<Operation> findAll() {
        List<OperationDocument> documents = operationRepository.findAll(MOST_RECENT_FIRST);
        Map<String, User> performers = referenceResolver.users(documents.stream()
                .map(OperationDocument::getPerformedById).filter(Objects::nonNull).toList(), User.class);
        return documents.stream()
                .map(document -> OperationMapper.toDomain(document, performers.get(document.getPerformedById())))
                .toList();
    }
}
