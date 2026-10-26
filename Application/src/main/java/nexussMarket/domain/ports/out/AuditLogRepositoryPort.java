package nexussMarket.domain.ports.out;

import java.util.List;

import nexussMarket.domain.models.AuditLog;

/** Append-only store of {@link AuditLog}s: entries are added, never updated nor deleted. */
public interface AuditLogRepositoryPort {

    /** Appends {@code auditLog}; an existing identifier is never overwritten. */
    AuditLog save(AuditLog auditLog);

    /** Every audit log entry, most recent first. */
    List<AuditLog> findAll();
}
