package nexussMarket.adapters.out.persistence.mongodb.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;

/** Spring Data repository for the append-only {@code audit_logs} collection. */
public interface AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String> {
}
