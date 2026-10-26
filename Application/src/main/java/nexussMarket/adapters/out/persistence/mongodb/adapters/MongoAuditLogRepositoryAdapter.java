package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.AuditLogMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.AuditLogMongoRepository;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.AuditLogRepositoryPort;

/**
 * Implements {@link AuditLogRepositoryPort} on the append-only
 * {@code audit_logs} collection: entries are only ever inserted. Reads resolve
 * every performer with a single query.
 */
@Repository
public class MongoAuditLogRepositoryAdapter implements AuditLogRepositoryPort {

    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "operationDate");

    private final AuditLogMongoRepository auditLogRepository;
    private final MongoReferenceResolver referenceResolver;

    public MongoAuditLogRepositoryAdapter(AuditLogMongoRepository auditLogRepository,
                                          MongoReferenceResolver referenceResolver) {
        this.auditLogRepository = auditLogRepository;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        auditLogRepository.insert(AuditLogMapper.toDocument(auditLog));
        return auditLog;
    }

    @Override
    public List<AuditLog> findAll() {
        List<AuditLogDocument> documents = auditLogRepository.findByOperationTypeNotNull(MOST_RECENT_FIRST);
        Map<String, User> performers = referenceResolver.users(documents.stream()
                .map(AuditLogDocument::getPerformedById).filter(Objects::nonNull).toList(), User.class);
        return documents.stream()
                .map(document -> AuditLogMapper.toDomain(document, performers.get(document.getPerformedById())))
                .toList();
    }
}
