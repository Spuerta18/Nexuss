package nexussMarket.adapters.out.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

import nexussMarket.adapters.out.persistence.mongodb.documents.AuditLogDocument;

/**
 * Spring Data repository for the append-only {@code audit_logs} collection.
 * It extends the bare {@link Repository} and declares only {@code insert} and
 * reads, so no update or delete method exists. {@code insert} is served by
 * the Spring Data base implementation and fails on an existing identifier.
 */
public interface AuditLogMongoRepository extends Repository<AuditLogDocument, String> {

    <S extends AuditLogDocument> S insert(S document);

    /** Entries of the domain audit model; legacy entries without {@code operationType} are skipped. */
    List<AuditLogDocument> findByOperationTypeNotNull(Sort sort);
}
