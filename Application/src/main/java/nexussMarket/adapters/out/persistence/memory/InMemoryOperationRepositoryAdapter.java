package nexussMarket.adapters.out.persistence.memory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.OperationDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.OperationMapper;
import nexussMarket.domain.models.Operation;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.OperationRepositoryPort;

/** {@link OperationRepositoryPort} on the in-memory {@code operations} collection (append-only). */
@Repository
@Profile("memory")
public class InMemoryOperationRepositoryAdapter implements OperationRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryOperationRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public Operation save(Operation entry) {
        database.operations.insert(OperationMapper.toDocument(entry), "Operation " + entry.getIdentifier() + " already exists");
        return entry;
    }

    /** Every entry, most recent first. */
    @Override
    public List<Operation> findAll() {
        List<OperationDocument> documents = database.operations.findAll().stream()
                .sorted(Comparator.comparing(OperationDocument::getExecutionDate).reversed()).toList();
        Map<String, User> performers = referenceResolver.users(documents.stream()
                .map(OperationDocument::getPerformedById).filter(Objects::nonNull).toList(), User.class);
        return documents.stream()
                .map(document -> OperationMapper.toDomain(document, performers.get(document.getPerformedById())))
                .toList();
    }
}
