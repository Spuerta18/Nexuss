package nexussMarket.adapters.out.persistence.memory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;
import nexussMarket.adapters.out.persistence.mongodb.mappers.AuditLogMapper;
import nexussMarket.domain.models.AuditLog;
import nexussMarket.domain.models.User;
import nexussMarket.domain.ports.out.AuditLogRepositoryPort;

/** {@link AuditLogRepositoryPort} on the in-memory {@code audit_logs} collection (append-only). */
@Repository
@Profile("memory")
public class InMemoryAuditLogRepositoryAdapter implements AuditLogRepositoryPort {

    private final InMemoryDatabase database;
    private final InMemoryReferenceResolver referenceResolver;

    public InMemoryAuditLogRepositoryAdapter(InMemoryDatabase database, InMemoryReferenceResolver referenceResolver) {
        this.database = database;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public AuditLog save(AuditLog entry) {
        database.auditLogs.insert(AuditLogMapper.toDocument(entry), "AuditLog " + entry.getAuditId() + " already exists");
        return entry;
    }

    /** Every entry, most recent first. */
    @Override
    public List<AuditLog> findAll() {
        List<AuditLogDocument> documents = database.auditLogs.findAll().stream()
                .sorted(Comparator.comparing(AuditLogDocument::getOperationDate).reversed()).toList();
        Map<String, User> performers = referenceResolver.users(documents.stream()
                .map(AuditLogDocument::getPerformedById).filter(Objects::nonNull).toList(), User.class);
        return documents.stream()
                .map(document -> AuditLogMapper.toDomain(document, performers.get(document.getPerformedById())))
                .toList();
    }
}
