package nexussMarket.adapters.out.persistence.mongodb.adapters;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import nexussMarket.adapters.out.persistence.mongodb.mappers.AuditLogMapper;
import nexussMarket.adapters.out.persistence.mongodb.repositories.AuditLogMongoRepository;
import nexussMarket.domain.ports.out.AuditLogPort;

/**
 * Implements {@link AuditLogPort} on the append-only {@code audit_logs}
 * collection: entries are only ever inserted.
 */
@Repository
public class MongoAuditLogAdapter implements AuditLogPort {

    private final AuditLogMongoRepository auditLogRepository;

    public MongoAuditLogAdapter(AuditLogMongoRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void record(String userId, String operation, LocalDateTime occurredAt) {
        auditLogRepository.insert(AuditLogMapper.toDocument(userId, operation, occurredAt));
    }
}
